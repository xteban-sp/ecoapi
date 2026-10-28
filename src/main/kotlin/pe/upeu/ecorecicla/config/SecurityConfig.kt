package pe.upeu.ecorecicla.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import pe.upeu.ecorecicla.seguridad.Privilegio.Companion.PESAJE_ANULAR
import pe.upeu.ecorecicla.seguridad.Privilegio.Companion.PESAJE_CREAR
import pe.upeu.ecorecicla.seguridad.Privilegio.Companion.PESAJE_EDITAR
import pe.upeu.ecorecicla.seguridad.Privilegio.Companion.PESAJE_LEER
import pe.upeu.ecorecicla.seguridad.Privilegio.Companion.PUNTO_GESTIONAR
import pe.upeu.ecorecicla.seguridad.Privilegio.Companion.PUNTO_LEER
import pe.upeu.ecorecicla.seguridad.Privilegio.Companion.RUTA_CERRAR

/**
 * Contrato de autorizacion del caso (seccion 3.3) en UN solo lugar: cada ruta
 * exige un PRIVILEGIO, nunca un nombre de rol. Que rol tiene que privilegio lo
 * decide la tabla rol_privilegio. Ningun controlador compara roles.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtService: JwtService,
    private val manejador: ManejadorDeErrores
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .logout { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Publicas
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                    .requestMatchers("/error").permitAll()

                    // Rutas del dia
                    .requestMatchers(HttpMethod.GET, "/api/v1/rutas/*/pesajes").hasAuthority(PESAJE_LEER)
                    .requestMatchers(HttpMethod.GET, "/api/v1/rutas/**").hasAuthority(PUNTO_LEER)
                    .requestMatchers(HttpMethod.POST, "/api/v1/rutas/*/cierre").hasAuthority(RUTA_CERRAR)
                    .requestMatchers(HttpMethod.PUT, "/api/v1/rutas/*/puntos/*/omision").hasAuthority(PESAJE_CREAR)

                    // Puntos de acopio
                    .requestMatchers(HttpMethod.GET, "/api/v1/puntos/*/pesajes").hasAuthority(PESAJE_LEER)
                    .requestMatchers(HttpMethod.GET, "/api/v1/puntos/**").hasAuthority(PUNTO_LEER)
                    .requestMatchers("/api/v1/puntos/**").hasAuthority(PUNTO_GESTIONAR)

                    // Pesajes
                    .requestMatchers(HttpMethod.GET, "/api/v1/pesajes/**").hasAuthority(PESAJE_LEER)
                    .requestMatchers(HttpMethod.POST, "/api/v1/pesajes").hasAuthority(PESAJE_CREAR)
                    .requestMatchers(HttpMethod.PUT, "/api/v1/pesajes/*").hasAuthority(PESAJE_EDITAR)
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/pesajes/*").hasAuthority(PESAJE_ANULAR)

                    // /auth/me y cualquier otra: basta con un token valido
                    .anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                // Sin token, token alterado o vencido -> 401
                ex.authenticationEntryPoint { request, response, _ ->
                    val motivo = request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_MOTIVO) as? String
                    manejador.escribir(
                        response, HttpStatus.UNAUTHORIZED,
                        motivo ?: "Debe iniciar sesion para acceder a este recurso", request.requestURI
                    )
                }
                // Token valido pero sin el privilegio -> 403 (la sesion sigue siendo valida)
                ex.accessDeniedHandler { request, response, _ ->
                    manejador.escribir(
                        response, HttpStatus.FORBIDDEN,
                        "No tiene el privilegio necesario para realizar esta operacion", request.requestURI
                    )
                }
            }
            .addFilterBefore(JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
