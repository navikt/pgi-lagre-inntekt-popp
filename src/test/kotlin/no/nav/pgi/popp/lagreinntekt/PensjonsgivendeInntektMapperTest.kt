package no.nav.pgi.popp.lagreinntekt


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pgi.popp.lagreinntekt.PensjonsgivendeInntektMapperTest.Companion.INNTEKT
import no.nav.pgi.popp.lagreinntekt.popp.PensjonsgivendeInntektDto
import no.nav.pgi.popp.lagreinntekt.popp.mapToPensjonsgivendeInntektDto
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import no.nav.samordning.pgi.schema.PensjonsgivendeInntektPerOrdning
import no.nav.samordning.pgi.schema.Skatteordning
import no.nav.samordning.pgi.schema.Skatteordning.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PensjonsgivendeInntektMapperTest {
    @Test
    fun `pensjonsgivende inntekt mapped to json`() {
        val pgiDto = createPensjonsgivendeInntekt(INDENTIFIKATOR, INNTEKTSAAR).mapToPensjonsgivendeInntektDto()
        val json = pgiDto.toJson()
        val parsedPgiDto: PensjonsgivendeInntektDto = ObjectMapper()
                .registerModule(KotlinModule())
                .readValue(json)

        assertEquals(pgiDto.inntektsaar, parsedPgiDto.inntektsaar)
        assertEquals(pgiDto.norskPersonidentifikator, parsedPgiDto.norskPersonidentifikator)

        val pgiPerOrdningDto1 = pgiDto.pensjonsgivendeInntekt[0]
        val pgiPerOrdningDto2 = pgiDto.pensjonsgivendeInntekt[1]
        val pgiPerOrdningDto3 = pgiDto.pensjonsgivendeInntekt[2]

        assertEquals("$INNTEKTSAAR-01-01", pgiPerOrdningDto1.datoForFastetting)
        assertEquals(INNTEKT, pgiPerOrdningDto1.pensjonsgivendeInntektAvLoennsinntekt)
        assertEquals(INNTEKT, pgiPerOrdningDto1.pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel)
        assertEquals(INNTEKT, pgiPerOrdningDto1.pensjonsgivendeInntektAvNaeringsinntekt)
        assertEquals(INNTEKT, pgiPerOrdningDto1.pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage)
        assertEquals("FASTLAND", pgiPerOrdningDto1.skatteordning)
        assertEquals("KILDESKATT_PAA_LOENN", pgiPerOrdningDto2.skatteordning)
        assertEquals("SVALBARD", pgiPerOrdningDto3.skatteordning)
    }

    companion object {
        const val INNTEKTSAAR = "2020"
        const val INDENTIFIKATOR = "12345678901"
        internal const val INNTEKT = 10L
    }
}

private fun createPensjonsgivendeInntekt(norskPersonidentifikator: String, inntektsaar: String): PensjonsgivendeInntekt {
    val pensjonsgivendeIntekter = mutableListOf<PensjonsgivendeInntektPerOrdning>()
            .apply {
                add(createPensjonsgivendeInntektPerOrdning(FASTLAND, inntektsaar))
                add(createPensjonsgivendeInntektPerOrdning(KILDESKATT_PAA_LOENN, inntektsaar))
                add(createPensjonsgivendeInntektPerOrdning(SVALBARD, inntektsaar))
            }
    return PensjonsgivendeInntekt(norskPersonidentifikator, inntektsaar, pensjonsgivendeIntekter)
}

private fun createPensjonsgivendeInntektPerOrdning(skatteOrdning: Skatteordning, inntektsaar: String): PensjonsgivendeInntektPerOrdning =
        PensjonsgivendeInntektPerOrdning(skatteOrdning, "$inntektsaar-01-01", INNTEKT, INNTEKT, INNTEKT, INNTEKT)