package pe.upeu.ecorecicla.seguridad

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

/** Un rol es solo un conjunto de privilegios; la asignacion vive en rol_privilegio. */
@Entity
@Table(name = "rol")
class Rol(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var nombre: String = "",
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "rol_privilegio",
        joinColumns = [JoinColumn(name = "rol_id")],
        inverseJoinColumns = [JoinColumn(name = "privilegio_id")]
    )
    var privilegios: MutableSet<Privilegio> = mutableSetOf()
)
