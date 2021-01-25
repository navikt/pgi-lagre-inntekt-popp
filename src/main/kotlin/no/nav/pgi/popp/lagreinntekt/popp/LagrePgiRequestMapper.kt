package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import no.nav.samordning.pgi.schema.PensjonsgivendeInntektPerOrdning

internal object LagrePgiRequestMapper {

    internal fun toLagrePgiRequest(pensjonsgivendeInntekt: PensjonsgivendeInntekt): LagrePgiRequest {
        return LagrePgiRequest(
            personIdentikator = pensjonsgivendeInntekt.getNorskPersonidentifikator(),
            inntektsaar = pensjonsgivendeInntekt.getInntektsaar().toString(),
            pgiOrdning = pensjonsgivendeInntekt.getPensjonsgivendeInntekt().map { toPgiOrdning(it) }
        )
    }

    private fun toPgiOrdning(pgiPerOrdning: PensjonsgivendeInntektPerOrdning) =
        PgiOrdning(
            skatteordning = pgiPerOrdning.getSkatteordning().name,
            datoForFastSetting = pgiPerOrdning.getDatoForFastsetting(),
            pgiLoenn = pgiPerOrdning.getPensjonsgivendeInntektAvLoennsinntekt(),
            pgiLoennPensjonsdel = pgiPerOrdning.getPensjonsgivendeInntektAvLoennsinntektBarePensjonsdel(),
            pgiNaering = pgiPerOrdning.getPensjonsgivendeInntektAvNaeringsinntekt(),
            pgiNaeringFiskeFangstFamiliebarnehage = pgiPerOrdning.getPensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage()
        )
}