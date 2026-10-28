package pe.upeu.ecorecicla.seguridad

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "usuario")
class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var username: String = "",
    /** Hash BCrypt. */
    var password: String = "",
    @Column(name = "nombre_completo")
    var nombreCompleto: String = "",
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id")
    var rol: Rol = Rol(),
    var activo: Boolean = true
) {
    fun privilegios(): Set<String> = rol.privilegios.map { it.nombre }.toSortedSet()
}

/**
 * Lo que el filtro JWT deja en el SecurityContext: identidad y privilegios
 * leidos del token ya verificado. Los servicios lo reciben para aplicar RN-02.
 */
data class UsuarioAutenticado(
    val id: Long,
    val username: String,
    val privilegios: Set<String>
) {
    fun tiene(privilegio: String): Boolean = privilegio in privilegios
}
