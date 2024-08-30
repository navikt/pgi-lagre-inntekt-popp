package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.pgi.domain.PensjonsgivendeInntekt
import no.nav.pgi.domain.PensjonsgivendeInntektMetadata
import no.nav.pgi.domain.PensjonsgivendeInntektPerOrdning
import no.nav.pgi.domain.Skatteordning
import no.nav.pgi.popp.lagreinntekt.popp.LagrePgiRequestMapper.toLagrePgiRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

        assertEquals(pensjonsgivendeInntekt.norskPersonidentifikator, lagrePgiRequest.personIdentifikator)
        assertEquals(pensjonsgivendeInntekt.inntektsaar.toInt(), lagrePgiRequest.inntektsaar)
        assertEquals(pensjonsgivendeInntekt.metaData.sekvensnummer, lagrePgiRequest.sekvensnummer)

        val pensjonsgivendeInntektFastland = pensjonsgivendeInntekt.getPensjonsgivendeInntekt(Skatteordning.FASTLAND)
        assertPensjonsgivendeInntektFastlandToPgiMapping(pensjonsgivendeInntektFastland, lagrePgiRequest)

        val pensjonsgivendeInntektSvalbard = pensjonsgivendeInntekt.getPensjonsgivendeInntekt(Skatteordning.SVALBARD)
        assertPensjonsgivendeInntektSvalbardToPgiMapping(pensjonsgivendeInntektSvalbard, lagrePgiRequest)

        val pensjonsgivendeInntektKSL =
            pensjonsgivendeInntekt.getPensjonsgivendeInntekt(Skatteordning.KILDESKATT_PAA_LOENN)
        assertPensjonsgivendeInntektKildeskattPaaLoennToPgiMapping(pensjonsgivendeInntektKSL, lagrePgiRequest)
    }

    @Test
    fun `maps from PensjonsgivendeInntekt to LagrePgiRequest when only FastlandsInntektLoenn has belop`() {
        val pensjonsgivendeInntekt = pensjonsgivendeInntekt(Skatteordning.FASTLAND, loenn = 100L, fff = 0L)
        val lagrePgiRequest = toLagrePgiRequest(pensjonsgivendeInntekt)

        val pensjonsgivendeInntektFastland = pensjonsgivendeInntekt.getPensjonsgivendeInntekt(Skatteordning.FASTLAND)
        assertEquals(1, lagrePgiRequest.pgiList.size)
        assertEquals(
            pensjonsgivendeInntektFastland.pensjonsgivendeInntektAvLoennsinntekt,
            lagrePgiRequest.getPgiBelop(InntektType.FL_PGI_LOENN)
        )
    }

    @Test
    fun `maps from PensjonsgivendeInntekt to LagrePgiRequest when FastlandsInntektLoenn has only null inntekter and SvalbardInntekt has BarePensjonsdel`() {
        val skatteOrdningFastland =
            PensjonsgivendeInntektPerOrdning(Skatteordning.FASTLAND, fastsettingDatoFastland, null, null, null, null)
        val skatteOrdningSvalbard =
            PensjonsgivendeInntektPerOrdning(Skatteordning.SVALBARD, fastsettingDatoFastland, 0L, 100L, null, null)
        val skatteordninger = listOf(skatteOrdningFastland, skatteOrdningSvalbard)
        val pensjonsgivendeInntekter = pensjonsgivendeInntekt(skatteordninger)

        val lagrePgiRequest = toLagrePgiRequest(pensjonsgivendeInntekter)

        val pensjonsgivendeInntektSvalbard = pensjonsgivendeInntekter.getPensjonsgivendeInntekt(Skatteordning.SVALBARD)

        assertEquals(2, lagrePgiRequest.pgiList.size)

        val defaultInntekt = 0L
        assertEquals(defaultInntekt, lagrePgiRequest.getPgiBelop(InntektType.FL_PGI_LOENN))

        assertEquals(
            pensjonsgivendeInntektSvalbard.pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel,
            lagrePgiRequest.getPgiBelop(InntektType.SVA_PGI_LOENN_PD)
        )
        assertNull(lagrePgiRequest.getPgiBelop(InntektType.SVA_PGI_LOENN))
    }

    @Test
    fun `maps from PensjonsgivendeInntekt to LagrePgiRequest when only FastlandsInntektLoenn and BarePensjonsdel has Belop 2`() {
        val pensjonsgivendeInntekt =
            pensjonsgivendeInntekt(Skatteordning.SVALBARD, loenn = 100L, loennBp = 200L, fff = 0L)
        val lagrePgiRequest = toLagrePgiRequest(pensjonsgivendeInntekt)

        val pensjonsgivendeInntektFastland = pensjonsgivendeInntekt.getPensjonsgivendeInntekt(Skatteordning.SVALBARD)
        assertEquals(2, lagrePgiRequest.pgiList.size)
        assertEquals(
            pensjonsgivendeInntektFastland.pensjonsgivendeInntektAvLoennsinntekt,
            lagrePgiRequest.getPgiBelop(InntektType.SVA_PGI_LOENN)
        )
        assertEquals(
            pensjonsgivendeInntektFastland.pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel,
            lagrePgiRequest.getPgiBelop(InntektType.SVA_PGI_LOENN_PD)
        )
    }

    @Test
    fun `when no inntekt has belop default loennsinntekt with belop 0 should be saved`() {
        val pensjonsgivendeInntekt = pensjonsgivendeInntekt(Skatteordning.FASTLAND, fff = 0L)
        val lagrePgiRequest = toLagrePgiRequest(pensjonsgivendeInntekt)

        assertEquals(1, lagrePgiRequest.pgiList.size)
        assertEquals(0L, lagrePgiRequest.getPgiBelop(InntektType.FL_PGI_LOENN))
    }

    @Test
    fun `when no sekvensnummer 0 should be used`() {
        val pensjonsgivendeInntekt =
            pensjonsgivendeInntekt(Skatteordning.SVALBARD, metadata = PensjonsgivendeInntektMetadata(0, 0))
        val lagrePgiRequest = toLagrePgiRequest(pensjonsgivendeInntekt)

        assertEquals(0L, lagrePgiRequest.sekvensnummer)
    }

    private fun assertPensjonsgivendeInntektFastlandToPgiMapping(
        pgiFastland: PensjonsgivendeInntektPerOrdning,
        lagrePgiRequest: LagrePgiRequest
    ) {
        assertEquals(
            pgiFastland.pensjonsgivendeInntektAvLoennsinntekt,
            lagrePgiRequest.getPgiBelop(InntektType.FL_PGI_LOENN)
        )
        assertEquals(
            pgiFastland.pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel,
            lagrePgiRequest.getPgiBelop(InntektType.FL_PGI_LOENN_PD)
        )
        assertEquals(
            pgiFastland.pensjonsgivendeInntektAvNaeringsinntekt,
            lagrePgiRequest.getPgiBelop(InntektType.FL_PGI_NAERING)
        )
        assertEquals(
            pgiFastland.pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage,
            lagrePgiRequest.getPgiBelop(InntektType.FL_PGI_NAERING_FFF)
        )
        assertEquals(4, lagrePgiRequest.countPgiWithDate(fastsettingDatoFastland))
    }

    private fun assertPensjonsgivendeInntektSvalbardToPgiMapping(
        pgiSvalbard: PensjonsgivendeInntektPerOrdning,
        lagrePgiRequest: LagrePgiRequest
    ) {
        assertEquals(
            pgiSvalbard.pensjonsgivendeInntektAvLoennsinntekt,
            lagrePgiRequest.getPgiBelop(InntektType.SVA_PGI_LOENN)
        )
        assertEquals(
            pgiSvalbard.pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel,
            lagrePgiRequest.getPgiBelop(InntektType.SVA_PGI_LOENN_PD)
        )
        assertEquals(
            pgiSvalbard.pensjonsgivendeInntektAvNaeringsinntekt,
            lagrePgiRequest.getPgiBelop(InntektType.SVA_PGI_NAERING)
        )
        assertEquals(
            pgiSvalbard.pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage,
            lagrePgiRequest.getPgiBelop(InntektType.SVA_PGI_NAERING_FFF)
        )
        assertEquals(4, lagrePgiRequest.countPgiWithDate(fastsettingDatoSvalbard))
    }

    private fun assertPensjonsgivendeInntektKildeskattPaaLoennToPgiMapping(
        pgiKildeskattPaaLoenn: PensjonsgivendeInntektPerOrdning,
        lagrePgiRequest: LagrePgiRequest
    ) {
        assertEquals(
            pgiKildeskattPaaLoenn.pensjonsgivendeInntektAvLoennsinntekt,
            lagrePgiRequest.getPgiBelop(InntektType.KSL_PGI_LOENN)
        )
        assertEquals(
            pgiKildeskattPaaLoenn.pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel,
            lagrePgiRequest.getPgiBelop(InntektType.KSL_PGI_LOENN_PD)
        )
        assertEquals(
            pgiKildeskattPaaLoenn.pensjonsgivendeInntektAvNaeringsinntekt,
            lagrePgiRequest.getPgiBelop(InntektType.KSL_PGI_NAERING)
        )
        assertEquals(
            pgiKildeskattPaaLoenn.pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage,
            lagrePgiRequest.getPgiBelop(InntektType.KSL_PGI_NAERING_FFF)
        )
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
            ),
            PensjonsgivendeInntektMetadata(1, 12L)
        )

    private fun pensjonsgivendeInntekt(
        skatteOrdning: Skatteordning,
        loenn: Long? = null,
        loennBp: Long? = null,
        naering: Long? = null,
        fff: Long? = null,
        metadata: PensjonsgivendeInntektMetadata = PensjonsgivendeInntektMetadata(1, 12L)
    ) =
        PensjonsgivendeInntekt(
            "12345678901",
            2020,
            listOf(
                PensjonsgivendeInntektPerOrdning(
                    skatteOrdning,
                    fastsettingDatoFastland,
                    loenn,
                    loennBp,
                    naering,
                    fff
                )
            ),
            metadata
        )

    private fun pensjonsgivendeInntekt(pgiPerOrdningList: List<PensjonsgivendeInntektPerOrdning>) =
        PensjonsgivendeInntekt(
            "12345678901",
            2020,
            pgiPerOrdningList,
            PensjonsgivendeInntektMetadata(0, 0)
        )
}

private fun LagrePgiRequest.getPgiBelop(inntektType: InntektType) =
    pgiList.find { it.inntektType == inntektType }?.belop

private fun LagrePgiRequest.countPgiWithDate(date: String) = pgiList.count { pgi -> pgi.datoForFastsetting == date }

private fun PensjonsgivendeInntekt.getPensjonsgivendeInntekt(skatteOrdning: Skatteordning): PensjonsgivendeInntektPerOrdning =
    pensjonsgivendeInntekt.find { it.skatteordning == skatteOrdning }!!