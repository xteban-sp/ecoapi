package pe.upeu.ecorecicla.dominio

/** 404: el recurso no existe. */
class RecursoNoEncontradoException(mensaje: String) : RuntimeException(mensaje)

/** 409: una regla de negocio del servidor no se cumple (RN-01, RN-04). */
class ReglaNegocioException(mensaje: String) : RuntimeException(mensaje)

/** 403: autenticado, pero la regla no le permite esta operacion concreta (RN-02). */
class SinPrivilegioException(mensaje: String) : RuntimeException(mensaje)

/** 400 con detalle por campo (RN-03). */
class ValidacionException(val errores: Map<String, String>) : RuntimeException("Datos invalidos") {
    constructor(campo: String, mensaje: String) : this(mapOf(campo to mensaje))
}
