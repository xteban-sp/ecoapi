package pe.upeu.ecorecicla.dominio

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import pe.upeu.ecorecicla.seguridad.Usuario
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "punto_acopio")
class PuntoAcopio(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "razon_social")
    var razonSocial: String = "",
    var direccion: String = "",
    var zona: String = "",
    @Column(name = "material_habitual")
    var materialHabitual: String = ""
)

enum class EstadoRuta { ABIERTA, CERRADA }

@Entity
@Table(name = "ruta")
class Ruta(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var fecha: LocalDate = LocalDate.now(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operario_id")
    var operario: Usuario? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    var supervisor: Usuario? = null,
    @Enumerated(EnumType.STRING)
    var estado: EstadoRuta = EstadoRuta.ABIERTA,
    @Column(name = "cerrada_en")
    var cerradaEn: LocalDateTime? = null,
    @OneToMany(mappedBy = "ruta", fetch = FetchType.LAZY)
    @OrderBy("secuencia ASC")
    var puntos: MutableList<RutaPunto> = mutableListOf()
) {
    fun estaCerrada(): Boolean = estado == EstadoRuta.CERRADA

    fun estaAsignadaA(usuarioId: Long): Boolean =
        operario?.id == usuarioId || supervisor?.id == usuarioId
}

/** Punto planificado en una ruta, con su orden de visita. */
@Entity
@Table(name = "ruta_punto")
class RutaPunto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id")
    var ruta: Ruta? = null,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "punto_id")
    var punto: PuntoAcopio = PuntoAcopio(),
    var secuencia: Int = 0,
    @Column(name = "motivo_omision")
    var motivoOmision: String? = null
)

@Entity
@Table(name = "pesaje")
class Pesaje(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id")
    var ruta: Ruta? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_id")
    var punto: PuntoAcopio? = null,
    @Column(name = "fecha_hora")
    var fechaHora: LocalDateTime = LocalDateTime.now(),
    var material: String = "",
    @Column(name = "peso_bruto", precision = 10, scale = 2)
    var pesoBruto: BigDecimal = BigDecimal.ZERO,
    @Column(precision = 10, scale = 2)
    var tara: BigDecimal = BigDecimal.ZERO,
    @Column(name = "peso_neto", precision = 10, scale = 2)
    var pesoNeto: BigDecimal = BigDecimal.ZERO,
    var observacion: String = "",
    /** SC-C: porcentaje de humedad estimada (0-100), opcional. */
    @Column(name = "humedad_estimada", precision = 5, scale = 2)
    var humedadEstimada: BigDecimal? = null,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operario_id")
    var operario: Usuario? = null,
    var anulado: Boolean = false,
    @Column(name = "motivo_anulacion")
    var motivoAnulacion: String? = null
)
