package no.nav.pgi.popp.lagreinntekt


import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PensjonsgivendeInntektMapperTest {

    @Test
    fun `pensjonsgivende inntekt mapped to json`() {
        val pensjongivendeInntekt = PensjonsgivendeInntekt(INDENTIFIKATOR, INNTEKTSAAR)
        val expectedJson = "\"{\\\"identifikator\\\": \\\"$INDENTIFIKATOR\\\", \\\"inntektsAar\\\": \\\"$INNTEKTSAAR\\\"}\""

        assertEquals(expectedJson, pensjongivendeInntekt.toJson())
    }

    companion object {
        const val INDENTIFIKATOR = "12345678901"
        const val INNTEKTSAAR = "2020"
    }
}