package pe.upeu.ecorecicla.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Los datos semilla dependen de la fecha (la "ruta del dia" es la de hoy). En PostgreSQL la base
 * persiste entre arranques, asi que en modo demostracion se limpia y se vuelve a migrar: cada
 * arranque deja la semilla del anexo 10 con la fecha actual. Con ecorecicla.reiniciar-datos=false
 * solo se aplican las migraciones pendientes y los datos se conservan.
 */
@Configuration
class ReinicioDeDatos(@Value("\${ecorecicla.reiniciar-datos}") private val reiniciar: Boolean) {

    @Bean
    fun estrategiaFlyway() = FlywayMigrationStrategy { flyway ->
        if (reiniciar) flyway.clean()
        flyway.migrate()
    }
}
