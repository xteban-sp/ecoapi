package pe.upeu.ecorecicla.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pe.upeu.ecorecicla.seguridad.Usuario
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado
import java.util.Date
import javax.crypto.SecretKey

/**
 * Emite y verifica el JWT. El token se firma con HMAC-SHA256 y un secreto que
 * solo conoce el servidor: cambiar un caracter invalida la firma.
 * No hay renovacion: al vencer (1 h) el cliente debe volver a iniciar sesion.
 */
@Service
class JwtService(
    @Value("\${ecorecicla.jwt.secreto}") secreto: String,
    @Value("\${ecorecicla.jwt.vigencia-segundos}") val vigenciaSegundos: Long
) {
    private val llave: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto))

    fun generar(usuario: Usuario): String {
        val ahora = System.currentTimeMillis()
        return Jwts.builder()
            .subject(usuario.username)
            .claim(CLAIM_ID, usuario.id)
            .claim(CLAIM_NOMBRE, usuario.nombreCompleto)
            .claim(CLAIM_ROL, usuario.rol.nombre)
            .claim(CLAIM_PRIVILEGIOS, usuario.privilegios().toList())
            .issuedAt(Date(ahora))
            .expiration(Date(ahora + vigenciaSegundos * 1000))
            .signWith(llave)
            .compact()
    }

    /**
     * Verifica firma y vencimiento. Lanza ExpiredJwtException si vencio y
     * JwtException si la firma no coincide o el token esta mal formado.
     */
    fun verificar(token: String): UsuarioAutenticado {
        val claims: Claims = Jwts.parser().verifyWith(llave).build().parseSignedClaims(token).payload
        val privilegios = (claims[CLAIM_PRIVILEGIOS] as? List<*>).orEmpty().map { it.toString() }.toSet()
        return UsuarioAutenticado(
            id = (claims[CLAIM_ID] as Number).toLong(),
            username = claims.subject,
            privilegios = privilegios
        )
    }

    companion object {
        const val CLAIM_ID = "uid"
        const val CLAIM_NOMBRE = "nombre"
        const val CLAIM_ROL = "rol"
        const val CLAIM_PRIVILEGIOS = "privilegios"
    }
}
