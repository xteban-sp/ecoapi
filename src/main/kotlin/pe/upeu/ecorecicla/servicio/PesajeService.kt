package pe.upeu.ecorecicla.servicio

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.ecorecicla.dominio.Pesaje
import pe.upeu.ecorecicla.dominio.RecursoNoEncontradoException
import pe.upeu.ecorecicla.dominio.ReglaNegocioException
import pe.upeu.ecorecicla.dominio.Ruta
import pe.upeu.ecorecicla.dominio.SinPrivilegioException
import pe.upeu.ecorecicla.dominio.ValidacionException
import pe.upeu.ecorecicla.repositorio.PesajeRepository
import pe.upeu.ecorecicla.repositorio.PuntoAcopioRepository
import pe.upeu.ecorecicla.repositorio.RutaPuntoRepository
import pe.upeu.ecorecicla.seguridad.Privilegio
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado
import pe.upeu.ecorecicla.seguridad.UsuarioRepository
import pe.upeu.ecorecicla.web.dto.PaginaResponse
import pe.upeu.ecorecicla.web.dto.PesajeCorreccionRequest
import pe.upeu.ecorecicla.web.dto.PesajeRequest
import pe.upeu.ecorecicla.web.dto.PesajeResponse
import pe.upeu.ecorecicla.web.dto.aRespuesta
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Coordina las operaciones sobre pesajes y hace cumplir RN-01, RN-02 y RN-03.
 * El privilegio general (PESAJE_CREAR, PESAJE_EDITAR...) ya lo verifico
 * SecurityConfig; aqui se aplican las reglas que dependen de los datos.
 */
@Service
class PesajeService(
    private val pesajes: PesajeRepository,
    private val puntos: PuntoAcopioRepository,
    private val rutaPuntos: RutaPuntoRepository,
    private val usuarios: UsuarioRepository,
    private val rutaService: RutaService
) {

    @Transactional(readOnly = true)
    fun obtener(id: Long): PesajeResponse = buscar(id).aRespuesta()

    /** Pesajes de un punto, del mas reciente al mas antiguo. */
    @Transactional(readOnly = true)
    fun listarPorPunto(puntoId: Long, pagina: Int, tamanio: Int): PaginaResponse<PesajeResponse> {
        if (!puntos.existsById(puntoId)) throw RecursoNoEncontradoException("El punto de acopio $puntoId no existe")
        return PaginaResponse.de(pesajes.findByPuntoIdOrderByFechaHoraDesc(puntoId, paginar(pagina, tamanio))) { it.aRespuesta() }
    }

    /** Todos los pesajes de una ruta: lo usa la sincronizacion de la aplicacion. */
    @Transactional(readOnly = true)
    fun listarPorRuta(rutaId: Long, usuario: UsuarioAutenticado, pagina: Int, tamanio: Int): PaginaResponse<PesajeResponse> {
        rutaService.buscarAsignada(rutaId, usuario)
        return PaginaResponse.de(pesajes.findByRutaIdOrderByFechaHoraDesc(rutaId, paginar(pagina, tamanio))) { it.aRespuesta() }
    }

    /** RF-B04. Aplica RN-01 (ruta cerrada) y RN-03 (neto > 0). */
    @Transactional
    fun registrar(peticion: PesajeRequest, usuario: UsuarioAutenticado): PesajeResponse {
        val ruta = rutaService.buscarAsignada(peticion.rutaId!!, usuario)
        verificarAbierta(ruta)
        val planificado = rutaPuntos.findByRutaIdAndPuntoId(ruta.id!!, peticion.puntoId!!)
            ?: throw RecursoNoEncontradoException("El punto ${peticion.puntoId} no pertenece a la ruta ${ruta.id}")

        val bruto = kilos(peticion.pesoBruto!!)
        val tara = kilos(peticion.tara!!)
        val pesaje = Pesaje(
            ruta = ruta,
            punto = planificado.punto,
            fechaHora = LocalDateTime.now(),
            material = peticion.material!!.trim().uppercase(),
            pesoBruto = bruto,
            tara = tara,
            pesoNeto = calcularNeto(bruto, tara),
            observacion = peticion.observacion?.trim().orEmpty(),
            operario = usuarios.getReferenceById(usuario.id)
        )
        return pesajes.save(pesaje).aRespuesta()
    }

    /** RF-B05. Aplica RN-01, RN-02 y RN-03. */
    @Transactional
    fun corregir(id: Long, peticion: PesajeCorreccionRequest, usuario: UsuarioAutenticado): PesajeResponse {
        val pesaje = buscar(id)
        verificarAbierta(pesaje.ruta!!)
        if (pesaje.anulado) throw ReglaNegocioException("No se puede corregir un pesaje anulado")
        verificarPropiedad(pesaje, usuario)

        val bruto = kilos(peticion.pesoBruto!!)
        val tara = kilos(peticion.tara!!)
        pesaje.pesoNeto = calcularNeto(bruto, tara)
        pesaje.pesoBruto = bruto
        pesaje.tara = tara
        pesaje.material = peticion.material!!.trim().uppercase()
        pesaje.observacion = peticion.observacion?.trim().orEmpty()
        return pesaje.aRespuesta()
    }

    /** RF-B05. La anulacion NO borra la fila: la marca. */
    @Transactional
    fun anular(id: Long, motivo: String?, usuario: UsuarioAutenticado) {
        val pesaje = buscar(id)
        verificarAbierta(pesaje.ruta!!)
        if (pesaje.anulado) throw ReglaNegocioException("El pesaje $id ya estaba anulado")
        pesaje.anulado = true
        pesaje.motivoAnulacion = motivo?.trim()?.takeIf { it.isNotEmpty() } ?: "Anulado por ${usuario.username}"
    }

    // ------------------------------------------------------------------ reglas

    /** RN-01: en una ruta cerrada no se registra ni corrige nada -> 409. */
    private fun verificarAbierta(ruta: Ruta) {
        if (ruta.estaCerrada()) {
            throw ReglaNegocioException("La ruta del ${ruta.fecha} ya esta cerrada: no admite registrar ni corregir pesajes")
        }
    }

    /**
     * RN-02: quien no tiene PESAJE_EDITAR_TODOS solo corrige lo que el mismo
     * registro y solo el mismo dia -> 403.
     */
    private fun verificarPropiedad(pesaje: Pesaje, usuario: UsuarioAutenticado) {
        if (usuario.tiene(Privilegio.PESAJE_EDITAR_TODOS)) return
        if (pesaje.operario?.id != usuario.id) {
            throw SinPrivilegioException("Solo puede corregir los pesajes que usted registro")
        }
        if (pesaje.fechaHora.toLocalDate() != LocalDate.now()) {
            throw SinPrivilegioException("Solo puede corregir sus pesajes dentro del mismo dia en que los registro")
        }
    }

    /** RN-03: el neto lo calcula el servicio y debe ser mayor que cero -> 400 en "tara". */
    private fun calcularNeto(bruto: BigDecimal, tara: BigDecimal): BigDecimal {
        val neto = bruto - tara
        if (neto.signum() <= 0) {
            throw ValidacionException("tara", "La tara debe ser menor que el peso bruto: el peso neto debe ser mayor que cero")
        }
        return neto
    }

    private fun buscar(id: Long): Pesaje =
        pesajes.findById(id).orElseThrow { RecursoNoEncontradoException("El pesaje $id no existe") }

    private fun kilos(valor: Double): BigDecimal = BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP)
}

/** Pagina y tamanio acotados: evita pedir 10 000 filas de golpe. */
fun paginar(pagina: Int, tamanio: Int): PageRequest =
    PageRequest.of(pagina.coerceAtLeast(0), tamanio.coerceIn(1, 200))
