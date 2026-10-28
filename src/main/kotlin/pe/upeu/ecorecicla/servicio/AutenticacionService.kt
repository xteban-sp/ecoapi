package pe.upeu.ecorecicla.servicio

import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.ecorecicla.config.JwtService
import pe.upeu.ecorecicla.dominio.RecursoNoEncontradoException
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado
import pe.upeu.ecorecicla.seguridad.UsuarioRepository
import pe.upeu.ecorecicla.web.dto.LoginResponse
import pe.upeu.ecorecicla.web.dto.UsuarioActualResponse

@Service
class AutenticacionService(
    private val usuarios: UsuarioRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService
) {

    /** Compara contra el hash BCrypt; el mismo 401 para usuario inexistente o clave incorrecta. */
    @Transactional(readOnly = true)
    fun iniciarSesion(username: String, password: String): LoginResponse {
        val usuario = usuarios.findByUsername(username.trim())
        if (usuario == null || !usuario.activo || !encoder.matches(password, usuario.password)) {
            throw BadCredentialsException("Credenciales incorrectas")
        }
        return LoginResponse(
            token = jwt.generar(usuario),
            expiraEnSegundos = jwt.vigenciaSegundos,
            username = usuario.username,
            nombreCompleto = usuario.nombreCompleto,
            rol = usuario.rol.nombre,
            privilegios = usuario.privilegios().toList()
        )
    }

    @Transactional(readOnly = true)
    fun usuarioActual(autenticado: UsuarioAutenticado): UsuarioActualResponse {
        val usuario = usuarios.findById(autenticado.id)
            .orElseThrow { RecursoNoEncontradoException("El usuario del token ya no existe") }
        return UsuarioActualResponse(
            id = usuario.id!!,
            username = usuario.username,
            nombreCompleto = usuario.nombreCompleto,
            rol = usuario.rol.nombre,
            privilegios = usuario.privilegios().toList()
        )
    }
}
