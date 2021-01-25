package no.nav.pgi.popp.lagreinntekt.popp

internal data class LagrePgiRequest(
    val personIdentikator: String,
    val inntektsaar: String,
    val pgiOrdning: List<PgiOrdning>
)

internal data class PgiOrdning(
    val skatteordning: String,
    val datoForFastSetting: String,
    val pgiLoenn: Long,
    val pgiLoennPensjonsdel: Long,
    val pgiNaering: Long,
    val pgiNaeringFiskeFangstFamiliebarnehage: Long
)