package pe.upeu.ecorecicla.seguridad

import org.springframework.data.jpa.repository.JpaRepository

interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun findByUsername(username: String): Usuario?
}
