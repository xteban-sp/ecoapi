package pe.upeu.ecorecicla.web

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado
import pe.upeu.ecorecicla.servicio.PesajeService
import pe.upeu.ecorecicla.servicio.PuntoAcopioService
import pe.upeu.ecorecicla.servicio.RutaService
import pe.upeu.ecorecicla.web.dto.OmisionRequest
import pe.upeu.ecorecicla.web.dto.PaginaResponse
import pe.upeu.ecorecicla.web.dto.PesajeCorreccionRequest
import pe.upeu.ecorecicla.web.dto.PesajeRequest
import pe.upeu.ecorecicla.web.dto.PesajeResponse
import pe.upeu.ecorecicla.web.dto.PuntoAcopioRequest
import pe.upeu.ecorecicla.web.dto.PuntoAcopioResponse
import pe.upeu.ecorecicla.web.dto.RutaResponse
import java.time.LocalDate

// Los controladores solo traducen HTTP <-> servicio. No comparan roles ni aplican
// reglas: el privilegio lo exige SecurityConfig y las reglas viven en servicio/.

@RestController
@RequestMapping("/api/v1/rutas")
class RutaController(
    private val rutas: RutaService,
    private val pesajes: PesajeService
) {

    /** RF-B02. GET /api/v1/rutas/del-dia?fecha=2026-10-27 (sin fecha: hoy). */
    @GetMapping("/del-dia")
    fun rutaDelDia(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fecha: LocalDate?
    ): RutaResponse = rutas.rutaDelDia(usuario, fecha ?: LocalDate.now())

    @GetMapping("/{id}/pesajes")
    fun pesajesDeRuta(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int
    ): PaginaResponse<PesajeResponse> = pesajes.listarPorRuta(id, usuario, pagina, tamanio)

    /** RF-B06. Cierre: accion sobre la ruta, no idempotente -> POST al subrecurso. */
    @PostMapping("/{id}/cierre")
    fun cerrar(@AuthenticationPrincipal usuario: UsuarioAutenticado, @PathVariable id: Long): RutaResponse =
        rutas.cerrar(id, usuario)

    @PutMapping("/{id}/puntos/{puntoId}/omision")
    fun omitir(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @PathVariable id: Long,
        @PathVariable puntoId: Long,
        @Valid @RequestBody peticion: OmisionRequest
    ): RutaResponse = rutas.omitirPunto(id, puntoId, peticion.motivo!!, usuario)
}

@RestController
@RequestMapping("/api/v1/puntos")
class PuntoAcopioController(
    private val puntos: PuntoAcopioService,
    private val pesajes: PesajeService
) {

    /** RF-B03. GET /api/v1/puntos?zona=NORTE&pagina=0&tamanio=20 */
    @GetMapping
    fun listar(
        @RequestParam(required = false) zona: String?,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int
    ): PaginaResponse<PuntoAcopioResponse> = puntos.listar(zona, pagina, tamanio)

    @PostMapping
    fun crear(@Valid @RequestBody peticion: PuntoAcopioRequest): ResponseEntity<PuntoAcopioResponse> {
        val creado = puntos.crear(peticion)
        return ResponseEntity.created(ubicacion(creado.id)).body(creado)
    }

    @GetMapping("/{id}/pesajes")
    fun pesajesDelPunto(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int
    ): PaginaResponse<PesajeResponse> = pesajes.listarPorPunto(id, pagina, tamanio)
}

@RestController
@RequestMapping("/api/v1/pesajes")
class PesajeController(private val pesajes: PesajeService) {

    @GetMapping("/{id}")
    fun obtener(@PathVariable id: Long): PesajeResponse = pesajes.obtener(id)

    /** RF-B04: 201 Created + Location. */
    @PostMapping
    fun registrar(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @Valid @RequestBody peticion: PesajeRequest
    ): ResponseEntity<PesajeResponse> {
        val creado = pesajes.registrar(peticion, usuario)
        return ResponseEntity.created(ubicacion(creado.id)).body(creado)
    }

    /** RF-B05: correccion -> 200. */
    @PutMapping("/{id}")
    fun corregir(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @PathVariable id: Long,
        @Valid @RequestBody peticion: PesajeCorreccionRequest
    ): PesajeResponse = pesajes.corregir(id, peticion, usuario)

    /** RF-B05: anulacion logica -> 204 sin cuerpo. La fila queda en la base marcada como anulada. */
    @DeleteMapping("/{id}")
    fun anular(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @PathVariable id: Long,
        @RequestParam(required = false) motivo: String?
    ): ResponseEntity<Void> {
        pesajes.anular(id, motivo, usuario)
        return ResponseEntity.noContent().build()
    }
}

private fun ubicacion(id: Long) =
    ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri()
