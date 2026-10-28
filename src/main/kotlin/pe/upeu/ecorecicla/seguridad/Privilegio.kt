package pe.upeu.ecorecicla.seguridad

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Permiso atomico del sistema. La autorizacion SIEMPRE se verifica contra un
 * privilegio, nunca contra el nombre del rol.
 */
@Entity
@Table(name = "privilegio")
class Privilegio(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var nombre: String = ""
) {
    /** Nombres tal como estan en la tabla privilegio; los usa SecurityConfig. */
    companion object {
        const val PUNTO_LEER = "PUNTO_LEER"
        const val PESAJE_LEER = "PESAJE_LEER"
        const val PESAJE_CREAR = "PESAJE_CREAR"
        const val PESAJE_EDITAR = "PESAJE_EDITAR"
        /** Columna "Todos" de PESAJE_EDITAR: corrige pesajes ajenos y de otros dias (RN-02). */
        const val PESAJE_EDITAR_TODOS = "PESAJE_EDITAR_TODOS"
        const val PESAJE_ANULAR = "PESAJE_ANULAR"
        const val RUTA_CERRAR = "RUTA_CERRAR"
        const val PUNTO_GESTIONAR = "PUNTO_GESTIONAR"
    }
}
