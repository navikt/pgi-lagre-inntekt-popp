package no.nav.pgi.popp.lagreinntekt.popp

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule


internal data class LagrePgiRequest(
    val personIdentifikator: String,
    val inntektsaar: Int,
    val sekvensnummer: Long,
    val pgiList: List<Pgi> = emptyList(),
)

internal data class Pgi(
    val inntektType: InntektType,
    val datoForFastsetting: String,
    val belop: Long?
)

internal enum class InntektType {
    FL_PGI_LOENN,
    FL_PGI_LOENN_PD,
    FL_PGI_NAERING,
    FL_PGI_NAERING_FFF,
    KSL_PGI_LOENN,
    KSL_PGI_LOENN_PD,
    KSL_PGI_NAERING,
    KSL_PGI_NAERING_FFF,
    SVA_PGI_LOENN,
    SVA_PGI_LOENN_PD,
    SVA_PGI_NAERING,
    SVA_PGI_NAERING_FFF
}

private val mapper = ObjectMapper().registerModule(KotlinModule())
internal fun LagrePgiRequest.toJson() = mapper.writeValueAsString(this)