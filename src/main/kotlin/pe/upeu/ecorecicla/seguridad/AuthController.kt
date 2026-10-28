package pe.upeu.ecorecicla.seguridad

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pe.upeu.ecorecicla.servicio.AutenticacionService
import pe.upeu.ecorecicla.web.dto.LoginRequest
import pe.upeu.ecorecicla.web.dto.LoginResponse
import pe.upeu.ecorecicla.web.dto.UsuarioActualResponse

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val autenticacion: AutenticacionService) {

    /** RF-B01. Unica ruta publica del servicio. Credenciales incorrectas: 401. */
    @PostMapping("/login")
    fun login(@Valid @RequestBody peticion: LoginRequest): LoginResponse =
        autenticacion.iniciarSesion(peticion.username!!, peticion.password!!)

    /** RF-B07. Solo exige un token valido. */
    @GetMapping("/me")
    fun usuarioActual(@AuthenticationPrincipal usuario: UsuarioAutenticado): UsuarioActualResponse =
        autenticacion.usuarioActual(usuario)
}
