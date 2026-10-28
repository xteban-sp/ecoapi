package pe.upeu.ecorecicla.config

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Lee "Authorization: Bearer <token>", verifica la firma y deja en el contexto
 * al usuario con sus privilegios como authorities. Si el token falta, esta
 * alterado o vencio, la peticion sigue SIN autenticar y SecurityConfig la
 * rechaza con 401; el motivo viaja en un atributo para el mensaje de error.
 */
class JwtAuthenticationFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val cabecera = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            try {
                val usuario = jwtService.verificar(cabecera.removePrefix(PREFIJO).trim())
                val authorities = usuario.privilegios.map { SimpleGrantedAuthority(it) }
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(usuario, null, authorities)
            } catch (e: ExpiredJwtException) {
                request.setAttribute(ATRIBUTO_MOTIVO, "El token ha vencido; inicie sesion nuevamente")
            } catch (e: JwtException) {
                request.setAttribute(ATRIBUTO_MOTIVO, "El token no es valido")
            } catch (e: IllegalArgumentException) {
                request.setAttribute(ATRIBUTO_MOTIVO, "El token no es valido")
            }
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        private const val PREFIJO = "Bearer "
        const val ATRIBUTO_MOTIVO = "ecorecicla.motivo401"
    }
}
