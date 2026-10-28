package pe.upeu.ecorecicla.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import pe.upeu.ecorecicla.dominio.Pesaje
import pe.upeu.ecorecicla.dominio.PuntoAcopio
import pe.upeu.ecorecicla.dominio.Ruta
import pe.upeu.ecorecicla.dominio.RutaPunto

// Los campos de las peticiones son anulables: un campo ausente produce 400 con
// detalle (validationErrors) en lugar de un error de deserializacion generico.

// ---------------------------------------------------------------- Autenticacion

data class LoginRequest(
    @field:NotBlank(message = "El usuario es obligatorio")
    val username: String?,
    @field:NotBlank(message = "La contrasena es obligatoria")
    val password: String?
)

data class LoginResponse(
    val token: String,
    val tipo: String = "Bearer",
    val expiraEnSegundos: Long,
    val username: String,
    val nombreCompleto: String,
    val rol: String,
    val privilegios: List<String>
)

data class UsuarioActualResponse(
    val id: Long,
    val username: String,
    val nombreCompleto: String,
    val rol: String,
    val privilegios: List<String>
)

// ---------------------------------------------------------------- Rutas y puntos

data class PuntoRutaResponse(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String,
    val secuencia: Int,
    val atendido: Boolean,
    val motivoOmision: String?
)

data class RutaResponse(
    val id: Long,
    val fecha: String,
    val estado: String,
    val operario: String,
    val supervisor: String,
    val puntos: List<PuntoRutaResponse>
) {
    companion object {
        fun de(ruta: Ruta, puntos: List<PuntoRutaResponse>) = RutaResponse(
            id = ruta.id!!,
            fecha = ruta.fecha.toString(),
            estado = ruta.estado.name,
            operario = ruta.operario?.username.orEmpty(),
            supervisor = ruta.supervisor?.username.orEmpty(),
            puntos = puntos
        )
    }
}

fun RutaPunto.aRespuesta(atendido: Boolean) = PuntoRutaResponse(
    id = punto.id!!,
    razonSocial = punto.razonSocial,
    direccion = punto.direccion,
    zona = punto.zona,
    materialHabitual = punto.materialHabitual,
    secuencia = secuencia,
    atendido = atendido,
    motivoOmision = motivoOmision
)

data class PuntoAcopioResponse(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String
)

fun PuntoAcopio.aRespuesta() = PuntoAcopioResponse(id!!, razonSocial, direccion, zona, materialHabitual)

data class PuntoAcopioRequest(
    @field:NotBlank(message = "La razon social es obligatoria")
    @field:Size(max = 150, message = "Maximo 150 caracteres")
    val razonSocial: String?,
    @field:NotBlank(message = "La direccion es obligatoria")
    val direccion: String?,
    @field:NotBlank(message = "La zona es obligatoria")
    val zona: String?,
    @field:NotBlank(message = "El material habitual es obligatorio")
    val materialHabitual: String?
)

data class OmisionRequest(
    @field:NotBlank(message = "El motivo de omision es obligatorio")
    @field:Size(max = 200, message = "Maximo 200 caracteres")
    val motivo: String?
)

// ---------------------------------------------------------------- Pesajes

/** El cliente NUNCA envia el peso neto: lo calcula el servicio (RN-03). */
data class PesajeRequest(
    @field:NotNull(message = "La ruta es obligatoria")
    val rutaId: Long?,
    @field:NotNull(message = "El punto de acopio es obligatorio")
    val puntoId: Long?,
    @field:NotBlank(message = "El tipo de material es obligatorio")
    @field:Size(max = 40, message = "Maximo 40 caracteres")
    val material: String?,
    @field:NotNull(message = "El peso bruto es obligatorio")
    @field:Positive(message = "El peso bruto debe ser mayor que cero")
    val pesoBruto: Double?,
    @field:NotNull(message = "La tara es obligatoria")
    @field:PositiveOrZero(message = "La tara no puede ser negativa")
    val tara: Double?,
    @field:Size(max = 250, message = "Maximo 250 caracteres")
    val observacion: String? = null
)

/** Correccion: el punto y la ruta de un pesaje no cambian. */
data class PesajeCorreccionRequest(
    @field:NotBlank(message = "El tipo de material es obligatorio")
    @field:Size(max = 40, message = "Maximo 40 caracteres")
    val material: String?,
    @field:NotNull(message = "El peso bruto es obligatorio")
    @field:Positive(message = "El peso bruto debe ser mayor que cero")
    val pesoBruto: Double?,
    @field:NotNull(message = "La tara es obligatoria")
    @field:PositiveOrZero(message = "La tara no puede ser negativa")
    val tara: Double?,
    @field:Size(max = 250, message = "Maximo 250 caracteres")
    val observacion: String? = null
)

data class PesajeResponse(
    val id: Long,
    val rutaId: Long,
    val puntoId: Long,
    val fechaHora: String,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,
    val observacion: String,
    val operario: String,
    val anulado: Boolean,
    val motivoAnulacion: String?
)

fun Pesaje.aRespuesta() = PesajeResponse(
    id = id!!,
    rutaId = ruta!!.id!!,
    puntoId = punto!!.id!!,
    fechaHora = fechaHora.withNano(0).toString(),
    material = material,
    pesoBruto = pesoBruto.toDouble(),
    tara = tara.toDouble(),
    pesoNeto = pesoNeto.toDouble(),
    observacion = observacion,
    operario = operario?.username.orEmpty(),
    anulado = anulado,
    motivoAnulacion = motivoAnulacion
)
