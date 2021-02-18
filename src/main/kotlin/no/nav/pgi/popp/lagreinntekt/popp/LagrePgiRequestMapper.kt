package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import no.nav.samordning.pgi.schema.PensjonsgivendeInntektPerOrdning
import no.nav.samordning.pgi.schema.Skatteordning


object LagrePgiRequestMapper {

    internal fun toLagrePgiRequest(pensjonsgivendeInntekt: PensjonsgivendeInntekt): LagrePgiRequest {
        return LagrePgiRequest(
            personIdentifikator = pensjonsgivendeInntekt.getNorskPersonidentifikator(),
            inntektsaar = pensjonsgivendeInntekt.getInntektsaar().toInt(),
            pgiList = pensjonsgivendeInntekt.getPensjonsgivendeInntekt().map { toPgiList(it) }.flatten()
        )
    }

    private fun toPgiList(pensjonsGivendeInntekt: PensjonsgivendeInntektPerOrdning): List<Pgi> {
        return listOf(
            toPgi(
                createPgiType(pensjonsGivendeInntekt.getSkatteordning(), PgiTypeUtenOrdning.PGI_LOENN),
                pensjonsGivendeInntekt.getDatoForFastsetting(),
                pensjonsGivendeInntekt.getPensjonsgivendeInntektAvLoennsinntekt()
            ),
            toPgi(
                createPgiType(pensjonsGivendeInntekt.getSkatteordning(), PgiTypeUtenOrdning.PGI_LOENN_PD),
                pensjonsGivendeInntekt.getDatoForFastsetting(),
                pensjonsGivendeInntekt.getPensjonsgivendeInntektAvLoennsinntektBarePensjonsdel()
            ),
            toPgi(
                createPgiType(pensjonsGivendeInntekt.getSkatteordning(), PgiTypeUtenOrdning.PGI_NAERING),
                pensjonsGivendeInntekt.getDatoForFastsetting(),
                pensjonsGivendeInntekt.getPensjonsgivendeInntektAvNaeringsinntekt()
            ),
            toPgi(
                createPgiType(pensjonsGivendeInntekt.getSkatteordning(), PgiTypeUtenOrdning.PGI_NAERING_FFF),
                pensjonsGivendeInntekt.getDatoForFastsetting(),
                pensjonsGivendeInntekt.getPensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage()
            )
        )
    }

    private fun createPgiType(skatteordning: Skatteordning, pgiTypeUtenOrdning: PgiTypeUtenOrdning): PgiType {
        return when (skatteordning) {
            Skatteordning.SVALBARD -> {
                when (pgiTypeUtenOrdning) {
                    PgiTypeUtenOrdning.PGI_LOENN -> PgiType.SVA_PGI_LOENN
                    PgiTypeUtenOrdning.PGI_LOENN_PD -> PgiType.SVA_PGI_LOENN_PD
                    PgiTypeUtenOrdning.PGI_NAERING -> PgiType.SVA_PGI_NAERING
                    PgiTypeUtenOrdning.PGI_NAERING_FFF -> PgiType.SVA_PGI_NAERING_FFF
                }
            }
            Skatteordning.FASTLAND -> {
                when (pgiTypeUtenOrdning) {
                    PgiTypeUtenOrdning.PGI_LOENN -> PgiType.FL_PGI_LOENN
                    PgiTypeUtenOrdning.PGI_LOENN_PD -> PgiType.FL_PGI_LOENN_PD
                    PgiTypeUtenOrdning.PGI_NAERING -> PgiType.FL_PGI_NAERING
                    PgiTypeUtenOrdning.PGI_NAERING_FFF -> PgiType.FL_PGI_NAERING_FFF
                }
            }
            Skatteordning.KILDESKATT_PAA_LOENN -> {
                when (pgiTypeUtenOrdning) {
                    PgiTypeUtenOrdning.PGI_LOENN -> PgiType.KSL_PGI_LOENN
                    PgiTypeUtenOrdning.PGI_LOENN_PD -> PgiType.KSL_PGI_LOENN_PD
                    PgiTypeUtenOrdning.PGI_NAERING -> PgiType.KSL_PGI_NAERING
                    PgiTypeUtenOrdning.PGI_NAERING_FFF -> PgiType.KSL_PGI_NAERING_FFF
                }
            }
            else -> {
                throw UnknownSkatteOrdningException(skatteordning.name)
            }
        }
    }

    private fun toPgi(pgiType: PgiType, datoForFastsetting: String, belop: Long?) =
        Pgi(
            pgiType = pgiType,
            datoForFastsetting = datoForFastsetting,
            belop = belop
        )
}

internal class UnknownSkatteOrdningException(missingSkatteordning: String?) :
    Exception("""Cant find $missingSkatteordning in ${LagrePgiRequestMapper::class.simpleName} when mapping to POPP lagrePgiRequest. """)

private enum class PgiTypeUtenOrdning {
    PGI_LOENN,
    PGI_LOENN_PD,
    PGI_NAERING,
    PGI_NAERING_FFF,
}