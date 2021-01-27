package no.nav.pgi.popp.lagreinntekt.popp

import com.fasterxml.jackson.databind.ObjectMapper

internal data class LagrePgiRequest(
        val personIdentifikator: String,
        val inntektsaar: String,
        val pgiOrdninger: List<PgiOrdning> = emptyList(),
)

internal data class PgiOrdning(
        val skatteordning: String,
        val datoForFastSetting: String,
        val pgiLoenn: Long?,
        val pgiLoennPensjonsdel: Long?,
        val pgiNaering: Long?,
        val pgiNaeringFiskeFangstFamiliebarnehage: Long?,
)

private val mapper = ObjectMapper()
internal fun LagrePgiRequest.toJson() = mapper.writeValueAsString(this)
