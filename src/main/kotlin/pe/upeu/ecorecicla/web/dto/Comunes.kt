package pe.upeu.ecorecicla.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

/** Envoltorio de paginacion de TODOS los listados (seccion 4.2). */
data class PaginaResponse<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
) {
    companion object {
        fun <E : Any, T> de(page: Page<E>, convertir: (E) -> T) = PaginaResponse(
            contenido = page.content.map(convertir),
            pagina = page.number,
            tamanio = page.size,
            totalElementos = page.totalElements,
            totalPaginas = page.totalPages,
            ultima = page.isLast
        )
    }
}

/** Formato de error de todo el servicio (seccion 4.2). */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponseDto(
    val timestamp: String,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val validationErrors: Map<String, String>? = null
) {
    companion object {
        fun de(status: HttpStatus, mensaje: String, ruta: String, errores: Map<String, String>? = null) =
            ErrorResponseDto(
                timestamp = LocalDateTime.now().withNano(0).toString(),
                status = status.value(),
                error = status.reasonPhrase,
                message = mensaje,
                path = ruta,
                validationErrors = errores
            )
    }
}
