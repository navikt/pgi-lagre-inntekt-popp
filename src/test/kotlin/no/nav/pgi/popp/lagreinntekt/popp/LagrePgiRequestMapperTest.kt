package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.pgi.popp.lagreinntekt.popp.LagrePgiRequestMapper.toLagrePgiRequest
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import no.nav.samordning.pgi.schema.PensjonsgivendeInntektPerOrdning
import no.nav.samordning.pgi.schema.Skatteordning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LagrePgiRequestMapperTest {
    private var fastsettingDatoFastland = "2020-01-01"
    private var fastsettingDatoSvalbard = "2020-01-02"
    private var fastsettingDatoKildeskattPaaLoenn = "2020-01-03"

    @Test
    fun `maps from PensjonsgivendeInntekt to LagrePgiRequest`() {
        val pensjonsgivendeInntekt = pensjonsgivendeInntekt()
        val lagrePgiRequest = toLagrePgiRequest(pensjonsgivendeInntekt())

        assertEquals(pensjonsgivendeInntekt.getNorskPersonidentifikator(), lagrePgiRequest.personIdentifikator)
        assertEquals(pensjonsgivendeInntekt.getInntektsaar().toInt(), lagrePgiRequest.inntektsaar)

        val pensjonsgivendeInntektFastland = pensjonsgivendeInntekt.getPensjonsgivendeInntekt(Skatteordning.FASTLAND)
        assertPensjonsgivendeInntektFastlandToPgiMapping(pensjonsgivendeInntektFastland!!, lagrePgiRequest)

        val pensjonsgivendeInntektSvalbard = pensjonsgivendeInntekt.getPensjonsgivendeInntekt(Skatteordning.SVALBARD)
        assertPensjonsgivendeInntektSvalbardToPgiMapping(pensjonsgivendeInntektSvalbard!!, lagrePgiRequest)

        val pensjonsgivendeInntektKSL = pensjonsgivendeInntekt.getPensjonsgivendeInntekt(Skatteordning.KILDESKATT_PAA_LOENN)
        assertPensjonsgivendeInntektKildeskattPaaLoennToPgiMapping(pensjonsgivendeInntektKSL!!, lagrePgiRequest)
    }

    private fun assertPensjonsgivendeInntektFastlandToPgiMapping(
        pgiFastland: PensjonsgivendeInntektPerOrdning,
        lagrePgiRequest: LagrePgiRequest
    ) {
        assertEquals(pgiFastland.getPensjonsgivendeInntektAvLoennsinntekt(), lagrePgiRequest.getPgiBelop(PgiType.FL_PGI_LOENN))
        assertEquals(pgiFastland.getPensjonsgivendeInntektAvLoennsinntektBarePensjonsdel(), lagrePgiRequest.getPgiBelop(PgiType.FL_PGI_LOENN_PD))
        assertEquals(pgiFastland.getPensjonsgivendeInntektAvNaeringsinntekt(), lagrePgiRequest.getPgiBelop(PgiType.FL_PGI_NAERING))
        assertEquals(pgiFastland.getPensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage(), lagrePgiRequest.getPgiBelop(PgiType.FL_PGI_NAERING_FFF))
        assertEquals(4, lagrePgiRequest.countPgiWithDate(fastsettingDatoFastland))
    }

    private fun assertPensjonsgivendeInntektSvalbardToPgiMapping(
        pgiSvalbard: PensjonsgivendeInntektPerOrdning,
        lagrePgiRequest: LagrePgiRequest
    ) {
        assertEquals(pgiSvalbard.getPensjonsgivendeInntektAvLoennsinntekt(), lagrePgiRequest.getPgiBelop(PgiType.SVA_PGI_LOENN))
        assertEquals(pgiSvalbard.getPensjonsgivendeInntektAvLoennsinntektBarePensjonsdel(), lagrePgiRequest.getPgiBelop(PgiType.SVA_PGI_LOENN_PD))
        assertEquals(pgiSvalbard.getPensjonsgivendeInntektAvNaeringsinntekt(), lagrePgiRequest.getPgiBelop(PgiType.SVA_PGI_NAERING))
        assertEquals(pgiSvalbard.getPensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage(), lagrePgiRequest.getPgiBelop(PgiType.SVA_PGI_NAERING_FFF))
        assertEquals(4, lagrePgiRequest.countPgiWithDate(fastsettingDatoSvalbard))
    }

    private fun assertPensjonsgivendeInntektKildeskattPaaLoennToPgiMapping(
        pgiKildeskattPaaLoenn: PensjonsgivendeInntektPerOrdning,
        lagrePgiRequest: LagrePgiRequest
    ) {
        assertEquals(pgiKildeskattPaaLoenn.getPensjonsgivendeInntektAvLoennsinntekt(), lagrePgiRequest.getPgiBelop(PgiType.KSL_PGI_LOENN))
        assertEquals(pgiKildeskattPaaLoenn.getPensjonsgivendeInntektAvLoennsinntektBarePensjonsdel(), lagrePgiRequest.getPgiBelop(PgiType.KSL_PGI_LOENN_PD))
        assertEquals(pgiKildeskattPaaLoenn.getPensjonsgivendeInntektAvNaeringsinntekt(), lagrePgiRequest.getPgiBelop(PgiType.KSL_PGI_NAERING))
        assertEquals(pgiKildeskattPaaLoenn.getPensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage(), lagrePgiRequest.getPgiBelop(PgiType.KSL_PGI_NAERING_FFF))
        assertEquals(4, lagrePgiRequest.countPgiWithDate(fastsettingDatoKildeskattPaaLoenn))
    }


    private fun pensjonsgivendeInntekt() =
        PensjonsgivendeInntekt(
            "12345678901",
            2020,
            listOf(
                PensjonsgivendeInntektPerOrdning(
                    Skatteordning.FASTLAND,
                    fastsettingDatoFastland,
                    1L,
                    2L,
                    3L,
                    4L
                ),
                PensjonsgivendeInntektPerOrdning(
                    Skatteordning.SVALBARD,
                    fastsettingDatoSvalbard,
                    5L,
                    6L,
                    7L,
                    8L
                ),
                PensjonsgivendeInntektPerOrdning(
                    Skatteordning.KILDESKATT_PAA_LOENN,
                    fastsettingDatoKildeskattPaaLoenn,
                    9L,
                    10L,
                    11L,
                    12L
                )
            )
        )
}

private fun LagrePgiRequest.getPgiBelop(pgiType: PgiType) = pgiList.find { it.pgiType == pgiType }!!.belop

private fun LagrePgiRequest.countPgiWithDate(date: String) = pgiList.filter { pgi -> pgi.datoForFastsetting == date }.count()

private fun PensjonsgivendeInntekt.getPensjonsgivendeInntekt(skatteOrdning: Skatteordning): PensjonsgivendeInntektPerOrdning? = getPensjonsgivendeInntekt().find { it.getSkatteordning() == skatteOrdning }


