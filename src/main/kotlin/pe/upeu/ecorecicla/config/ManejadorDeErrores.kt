package pe.upeu.ecorecicla.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import pe.upeu.ecorecicla.dominio.RecursoNoEncontradoException
import pe.upeu.ecorecicla.dominio.ReglaNegocioException
import pe.upeu.ecorecicla.dominio.SinPrivilegioException
import pe.upeu.ecorecicla.dominio.ValidacionException
import pe.upeu.ecorecicla.web.dto.ErrorResponseDto
import tools.jackson.databind.ObjectMapper

/**
 * Traduce cada excepcion al formato de error comun (seccion 4.2). Es el unico
 * lugar que decide el codigo HTTP de un fallo.
 */
@RestControllerAdvice
class ManejadorDeErrores(private val mapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 400: anotaciones de validacion del DTO (@NotBlank, @Positive...). */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacionDto(ex: MethodArgumentNotValidException, req: HttpServletRequest) =
        responder(
            HttpStatus.BAD_REQUEST, "Datos invalidos", req,
            ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor invalido") }
        )

    /** 400: reglas de validacion del servicio (RN-03). */
    @ExceptionHandler(ValidacionException::class)
    fun validacionServicio(ex: ValidacionException, req: HttpServletRequest) =
        responder(HttpStatus.BAD_REQUEST, "Datos invalidos", req, ex.errores)

    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun cuerpoIlegible(ex: Exception, req: HttpServletRequest) =
        responder(HttpStatus.BAD_REQUEST, "El cuerpo o los parametros de la peticion no son validos", req)

    /** 401 solo en el login: usuario o contrasena incorrectos. */
    @ExceptionHandler(BadCredentialsException::class)
    fun credenciales(ex: BadCredentialsException, req: HttpServletRequest) =
        responder(HttpStatus.UNAUTHORIZED, "Usuario o contrasena incorrectos", req)

    /** 403: el servicio rechaza aunque el privilegio general exista (RN-02). */
    @ExceptionHandler(SinPrivilegioException::class)
    fun sinPrivilegio(ex: SinPrivilegioException, req: HttpServletRequest) =
        responder(HttpStatus.FORBIDDEN, ex.message ?: "Operacion no autorizada", req)

    @ExceptionHandler(AccessDeniedException::class)
    fun accesoDenegado(ex: AccessDeniedException, req: HttpServletRequest) =
        responder(HttpStatus.FORBIDDEN, "No tiene el privilegio necesario para realizar esta operacion", req)

    @ExceptionHandler(RecursoNoEncontradoException::class)
    fun noEncontrado(ex: RecursoNoEncontradoException, req: HttpServletRequest) =
        responder(HttpStatus.NOT_FOUND, ex.message ?: "Recurso no encontrado", req)

    /** 409: regla de negocio incumplida (RN-01, RN-04, pesaje anulado...). */
    @ExceptionHandler(ReglaNegocioException::class)
    fun reglaNegocio(ex: ReglaNegocioException, req: HttpServletRequest) =
        responder(HttpStatus.CONFLICT, ex.message ?: "Regla de negocio incumplida", req)

    /**
     * Excepciones propias de Spring MVC (ruta inexistente 404, metodo no
     * soportado 405...) conservan su codigo; el resto es 500 sin detalles internos.
     */
    @ExceptionHandler(Exception::class)
    fun inesperado(ex: Exception, req: HttpServletRequest): ResponseEntity<ErrorResponseDto> {
        if (ex is ErrorResponse) {
            val status = HttpStatus.resolve(ex.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
            return responder(status, ex.body.detail ?: status.reasonPhrase, req)
        }
        log.error("Error no controlado en {}", req.requestURI, ex)
        return responder(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocurrio un error inesperado. Intente nuevamente mas tarde.", req
        )
    }

    /** Usado por los manejadores de 401/403 de SecurityConfig, que actuan antes del controlador. */
    fun escribir(response: HttpServletResponse, status: HttpStatus, mensaje: String, ruta: String) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(mapper.writeValueAsString(ErrorResponseDto.de(status, mensaje, ruta)))
    }

    private fun responder(
        status: HttpStatus,
        mensaje: String,
        req: HttpServletRequest,
        errores: Map<String, String>? = null
    ): ResponseEntity<ErrorResponseDto> =
        ResponseEntity.status(status).body(ErrorResponseDto.de(status, mensaje, req.requestURI, errores))
}
