package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.pgi.popp.lagreinntekt.popp.LagrePgiRequestMapper.toLagrePgiRequest
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import no.nav.samordning.pgi.schema.PensjonsgivendeInntektPerOrdning
import no.nav.samordning.pgi.schema.Skatteordning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val personIdentifikator = "12345678901"
private const val inntektsaar = 2021L
private val skatteOrdningFastland = Skatteordning.FASTLAND
private const val datoForFastsetting = "01-04-2321"
private const val pensjonsgivendeInntektAvLoennsinntekt = 1L
private const val pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel = 2L
private const val pensjonsgivendeInntektAvNaeringsinntekt = 3L
private const val pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamilebarnehage = 4L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LagrePgiRequestMapperTest {

    @Test
    fun `maps from PensjonsgivendeInntekt to LagrePgiRequest`() {

        val pgiRequest = toLagrePgiRequest(pensjonsgivendeInntekt())
        assertEquals(personIdentifikator, pgiRequest.personIdentifikator)
        assertEquals(inntektsaar.toString(), pgiRequest.inntektsaar)
        assertEquals(skatteOrdningFastland.name, pgiRequest.pgiOrdninger.first().skatteordning)
        assertEquals(datoForFastsetting, pgiRequest.pgiOrdninger.first().datoForFastSetting)
    }

    private fun pensjonsgivendeInntekt() =
        PensjonsgivendeInntekt(
            personIdentifikator,
            inntektsaar,
            listOf(
                PensjonsgivendeInntektPerOrdning(
                    skatteOrdningFastland,
                    datoForFastsetting,
                    pensjonsgivendeInntektAvLoennsinntekt,
                    pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel,
                    pensjonsgivendeInntektAvNaeringsinntekt,
                    pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamilebarnehage
                )
            )
        )
}