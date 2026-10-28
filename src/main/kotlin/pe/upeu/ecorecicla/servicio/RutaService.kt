package pe.upeu.ecorecicla.servicio

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.ecorecicla.dominio.EstadoRuta
import pe.upeu.ecorecicla.dominio.RecursoNoEncontradoException
import pe.upeu.ecorecicla.dominio.ReglaNegocioException
import pe.upeu.ecorecicla.dominio.Ruta
import pe.upeu.ecorecicla.dominio.SinPrivilegioException
import pe.upeu.ecorecicla.repositorio.PesajeRepository
import pe.upeu.ecorecicla.repositorio.RutaPuntoRepository
import pe.upeu.ecorecicla.repositorio.RutaRepository
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado
import pe.upeu.ecorecicla.web.dto.RutaResponse
import pe.upeu.ecorecicla.web.dto.aRespuesta
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RutaService(
    private val rutas: RutaRepository,
    private val rutaPuntos: RutaPuntoRepository,
    private val pesajes: PesajeRepository
) {

    /** RF-B02: ruta asignada al usuario para la fecha, con puntos en orden de visita. */
    @Transactional(readOnly = true)
    fun rutaDelDia(usuario: UsuarioAutenticado, fecha: LocalDate): RutaResponse {
        val ruta = rutas.buscarAsignadas(fecha, usuario.id).firstOrNull()
            ?: throw RecursoNoEncontradoException("No tiene una ruta asignada para el $fecha")
        return respuesta(ruta)
    }

    /** RF-B06 / RN-04: no se cierra mientras quede un punto sin pesaje ni motivo de omision. */
    @Transactional
    fun cerrar(rutaId: Long, usuario: UsuarioAutenticado): RutaResponse {
        val ruta = buscarAsignada(rutaId, usuario)
        if (ruta.estaCerrada()) throw ReglaNegocioException("La ruta ya esta cerrada")
        val faltan = rutaPuntos.findByRutaIdOrderBySecuencia(rutaId).count { !atendido(rutaId, it.punto.id!!, it.motivoOmision) }
        if (faltan > 0) {
            throw ReglaNegocioException(
                "No se puede cerrar la ruta: faltan $faltan punto(s) sin pesaje registrado ni motivo de omision"
            )
        }
        ruta.estado = EstadoRuta.CERRADA
        ruta.cerradaEn = LocalDateTime.now()
        return respuesta(ruta)
    }

    /** Registra el motivo por el que un punto planificado no se pesa (habilita RN-04). */
    @Transactional
    fun omitirPunto(rutaId: Long, puntoId: Long, motivo: String, usuario: UsuarioAutenticado): RutaResponse {
        val ruta = buscarAsignada(rutaId, usuario)
        if (ruta.estaCerrada()) throw ReglaNegocioException("La ruta del ${ruta.fecha} ya esta cerrada")
        val planificado = rutaPuntos.findByRutaIdAndPuntoId(rutaId, puntoId)
            ?: throw RecursoNoEncontradoException("El punto $puntoId no pertenece a la ruta $rutaId")
        planificado.motivoOmision = motivo.trim()
        return respuesta(ruta)
    }

    /** Ruta existente (404) y asignada al usuario como operario o supervisor (403). */
    fun buscarAsignada(rutaId: Long, usuario: UsuarioAutenticado): Ruta {
        val ruta = rutas.findById(rutaId).orElseThrow { RecursoNoEncontradoException("La ruta $rutaId no existe") }
        if (!ruta.estaAsignadaA(usuario.id)) throw SinPrivilegioException("La ruta $rutaId no esta asignada a este usuario")
        return ruta
    }

    private fun respuesta(ruta: Ruta): RutaResponse {
        val puntos = rutaPuntos.findByRutaIdOrderBySecuencia(ruta.id!!).map {
            it.aRespuesta(atendido(ruta.id!!, it.punto.id!!, it.motivoOmision))
        }
        return RutaResponse.de(ruta, puntos)
    }

    private fun atendido(rutaId: Long, puntoId: Long, motivoOmision: String?): Boolean =
        motivoOmision != null || pesajes.existsByRutaIdAndPuntoIdAndAnuladoFalse(rutaId, puntoId)
}
