package pe.upeu.ecorecicla.servicio

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.ecorecicla.dominio.PuntoAcopio
import pe.upeu.ecorecicla.repositorio.PuntoAcopioRepository
import pe.upeu.ecorecicla.web.dto.PaginaResponse
import pe.upeu.ecorecicla.web.dto.PuntoAcopioRequest
import pe.upeu.ecorecicla.web.dto.PuntoAcopioResponse
import pe.upeu.ecorecicla.web.dto.aRespuesta

@Service
class PuntoAcopioService(private val puntos: PuntoAcopioRepository) {

    /** RF-B03: listado paginado con filtro opcional por zona. */
    @Transactional(readOnly = true)
    fun listar(zona: String?, pagina: Int, tamanio: Int): PaginaResponse<PuntoAcopioResponse> {
        val paginacion = paginar(pagina, tamanio).withSort(Sort.by("razonSocial"))
        val page = if (zona.isNullOrBlank()) puntos.findAll(paginacion) else puntos.findByZonaIgnoreCase(zona.trim(), paginacion)
        return PaginaResponse.de(page) { it.aRespuesta() }
    }

    @Transactional
    fun crear(peticion: PuntoAcopioRequest): PuntoAcopioResponse =
        puntos.save(
            PuntoAcopio(
                razonSocial = peticion.razonSocial!!.trim(),
                direccion = peticion.direccion!!.trim(),
                zona = peticion.zona!!.trim().uppercase(),
                materialHabitual = peticion.materialHabitual!!.trim().uppercase()
            )
        ).aRespuesta()
}
