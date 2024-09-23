package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.pgi.domain.PensjonsgivendeInntekt
import no.nav.pgi.domain.PensjonsgivendeInntektPerOrdning
import no.nav.pgi.domain.Skatteordning


object LagrePgiRequestMapper {

    internal fun toLagrePgiRequest(pensjonsgivendeInntekt: PensjonsgivendeInntekt): LagrePgiRequest {
        return LagrePgiRequest(
            personIdentifikator = pensjonsgivendeInntekt.norskPersonidentifikator,
            inntektsaar = pensjonsgivendeInntekt.inntektsaar.toInt(),
            pgiList = pensjonsgivendeInntekt.pensjonsgivendeInntekt.map { toPgiList(it) }.flatten(),
            sekvensnummer = pensjonsgivendeInntekt.metaData.sekvensnummer
        )
    }

    private fun toPgiList(pensjonsGivendeInntekt: PensjonsgivendeInntektPerOrdning): List<Pgi> {
        val pgiList = listOf(
            toPgi(
                createInntektType(pensjonsGivendeInntekt.skatteordning, PgiTypeUtenOrdning.PGI_LOENN),
                pensjonsGivendeInntekt.datoForFastsetting,
                pensjonsGivendeInntekt.pensjonsgivendeInntektAvLoennsinntekt
            ),
            toPgi(
                createInntektType(pensjonsGivendeInntekt.skatteordning, PgiTypeUtenOrdning.PGI_LOENN_PD),
                pensjonsGivendeInntekt.datoForFastsetting,
                pensjonsGivendeInntekt.pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel
            ),
            toPgi(
                createInntektType(pensjonsGivendeInntekt.skatteordning, PgiTypeUtenOrdning.PGI_NAERING),
                pensjonsGivendeInntekt.datoForFastsetting,
                pensjonsGivendeInntekt.pensjonsgivendeInntektAvNaeringsinntekt
            ),
            toPgi(
                createInntektType(pensjonsGivendeInntekt.skatteordning, PgiTypeUtenOrdning.PGI_NAERING_FFF),
                pensjonsGivendeInntekt.datoForFastsetting,
                pensjonsGivendeInntekt.pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage
            )
        ).filterNullInntekter()

        return pgiList.ifEmpty { defaultPgiListLoenn(pensjonsGivendeInntekt) }
        }
    }

    private fun List<Pgi>.filterNullInntekter() : List<Pgi>{
        return filter {it.belop != null && it.belop != 0L}
    }


    private fun defaultPgiListLoenn(pensjonsGivendeInntekt: PensjonsgivendeInntektPerOrdning) =
        listOf(
            toPgi(
                createInntektType(pensjonsGivendeInntekt.skatteordning, PgiTypeUtenOrdning.PGI_LOENN),
                pensjonsGivendeInntekt.datoForFastsetting,
                0L
            )
        )

    private fun createInntektType(skatteordning: Skatteordning, pgiTypeUtenOrdning: PgiTypeUtenOrdning): InntektType {
        return when (skatteordning) {
            Skatteordning.SVALBARD -> {
                when (pgiTypeUtenOrdning) {
                    PgiTypeUtenOrdning.PGI_LOENN -> InntektType.SVA_PGI_LOENN
                    PgiTypeUtenOrdning.PGI_LOENN_PD -> InntektType.SVA_PGI_LOENN_PD
                    PgiTypeUtenOrdning.PGI_NAERING -> InntektType.SVA_PGI_NAERING
                    PgiTypeUtenOrdning.PGI_NAERING_FFF -> InntektType.SVA_PGI_NAERING_FFF
                }
            }
            Skatteordning.FASTLAND -> {
                when (pgiTypeUtenOrdning) {
                    PgiTypeUtenOrdning.PGI_LOENN -> InntektType.FL_PGI_LOENN
                    PgiTypeUtenOrdning.PGI_LOENN_PD -> InntektType.FL_PGI_LOENN_PD
                    PgiTypeUtenOrdning.PGI_NAERING -> InntektType.FL_PGI_NAERING
                    PgiTypeUtenOrdning.PGI_NAERING_FFF -> InntektType.FL_PGI_NAERING_FFF
                }
            }
            Skatteordning.KILDESKATT_PAA_LOENN -> {
                when (pgiTypeUtenOrdning) {
                    PgiTypeUtenOrdning.PGI_LOENN -> InntektType.KSL_PGI_LOENN
                    PgiTypeUtenOrdning.PGI_LOENN_PD -> InntektType.KSL_PGI_LOENN_PD
                    PgiTypeUtenOrdning.PGI_NAERING -> InntektType.KSL_PGI_NAERING
                    PgiTypeUtenOrdning.PGI_NAERING_FFF -> InntektType.KSL_PGI_NAERING_FFF
                }
            }
            else -> {
                throw UnknownSkatteOrdningException(skatteordning.name)
            }
        }
    }

    private fun toPgi(inntektType: InntektType, datoForFastsetting: String, belop: Long?) =
        Pgi(
            inntektType = inntektType,
            datoForFastsetting = datoForFastsetting,
            belop = belop
        )

internal class UnknownSkatteOrdningException(missingSkatteordning: String?) : Exception("""Cant find $missingSkatteordning in ${LagrePgiRequestMapper::class.simpleName} when mapping to POPP lagrePgiRequest. """)

private enum class PgiTypeUtenOrdning {
    PGI_LOENN,
    PGI_LOENN_PD,
    PGI_NAERING,
    PGI_NAERING_FFF,
}