package pe.upeu.ecorecicla.repositorio

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pe.upeu.ecorecicla.dominio.Pesaje
import pe.upeu.ecorecicla.dominio.PuntoAcopio
import pe.upeu.ecorecicla.dominio.Ruta
import pe.upeu.ecorecicla.dominio.RutaPunto
import java.time.LocalDate

interface PuntoAcopioRepository : JpaRepository<PuntoAcopio, Long> {
    fun findByZonaIgnoreCase(zona: String, pageable: Pageable): Page<PuntoAcopio>
}

interface RutaRepository : JpaRepository<Ruta, Long> {
    @Query(
        "select r from Ruta r where r.fecha = :fecha " +
            "and (r.operario.id = :usuarioId or r.supervisor.id = :usuarioId) order by r.id"
    )
    fun buscarAsignadas(@Param("fecha") fecha: LocalDate, @Param("usuarioId") usuarioId: Long): List<Ruta>
}

interface RutaPuntoRepository : JpaRepository<RutaPunto, Long> {
    fun findByRutaIdOrderBySecuencia(rutaId: Long): List<RutaPunto>
    fun findByRutaIdAndPuntoId(rutaId: Long, puntoId: Long): RutaPunto?
}

interface PesajeRepository : JpaRepository<Pesaje, Long> {
    fun findByPuntoIdOrderByFechaHoraDesc(puntoId: Long, pageable: Pageable): Page<Pesaje>
    fun findByRutaIdOrderByFechaHoraDesc(rutaId: Long, pageable: Pageable): Page<Pesaje>
    fun existsByRutaIdAndPuntoIdAndAnuladoFalse(rutaId: Long, puntoId: Long): Boolean
}
