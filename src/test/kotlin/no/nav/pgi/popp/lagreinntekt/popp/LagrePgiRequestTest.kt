package no.nav.pgi.popp.lagreinntekt.popp

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.samordning.pgi.schema.Skatteordning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private val mapper = ObjectMapper().registerModule(KotlinModule()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

private const val PERSON_IDENTIFIKATOR = "12345678901"
private const val INNTEKTSAAR = "2021"

private val FASTLAND = Skatteordning.FASTLAND.name
private val KILDESKATT_PAA_LOENN = Skatteordning.KILDESKATT_PAA_LOENN.name
private val SVALBARD = Skatteordning.SVALBARD.name

private const val DATOFOR_FASTSETTING = "01-04-2321"
private const val PGI_LOENN = 1L
private const val PGI_LOENN_PENSJONSDEL = 2L
private const val PGI_NAERING = 3L
private const val PGI_NAERING_FISKE_FANGST_FAMILIEBARNEHAGE = 4L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LagrePgiRequestTest {

    @Test
    fun `map LagrePgiRequest to json string`() {
        val request = createLagrePgiRequest()
        val convertedRequest = request.toJson().toLagrePgiRequest()

        assertEquals(request.personIdentifikator, convertedRequest.personIdentifikator)
        assertEquals(request.inntektsaar, convertedRequest.inntektsaar)
    }

    @Test
    fun `map PgiOrdninger to json string`() {
        val request = createLagrePgiRequest(
                pgiOrdninger = listOf(
                        createPgiOrdning(FASTLAND),
                        createPgiOrdning(KILDESKATT_PAA_LOENN),
                        createPgiOrdning(SVALBARD)
                )
        )
        val convertedRequest = request.toJson().toLagrePgiRequest()

        assertEquals(request.pgiOrdninger.size, convertedRequest.pgiOrdninger.size)
        assertEquals(request.pgiOrdninger.getSkatteordning(FASTLAND), convertedRequest.pgiOrdninger.getSkatteordning(FASTLAND))
        assertEquals(request.pgiOrdninger.getSkatteordning(KILDESKATT_PAA_LOENN), convertedRequest.pgiOrdninger.getSkatteordning(KILDESKATT_PAA_LOENN))
        assertEquals(request.pgiOrdninger.getSkatteordning(SVALBARD), convertedRequest.pgiOrdninger.getSkatteordning(SVALBARD))
    }

    private fun createLagrePgiRequest(
            identifikator: String = PERSON_IDENTIFIKATOR,
            aar: String = INNTEKTSAAR,
            pgiOrdninger: List<PgiOrdning> = listOf(createPgiOrdning()),
    ) = LagrePgiRequest(
            personIdentifikator = identifikator,
            inntektsaar = aar,
            pgiOrdninger = pgiOrdninger
    )

    private fun createPgiOrdning(
            skatteordning: String = FASTLAND,
            datoForFastSetting: String = DATOFOR_FASTSETTING,
            pgiLoenn: Long? = PGI_LOENN,
            pgiLoennPensjonsdel: Long? = PGI_LOENN_PENSJONSDEL,
            pgiNaering: Long? = PGI_NAERING,
            pgiNaeringFiskeFangstFamiliebarnehage: Long? = PGI_NAERING_FISKE_FANGST_FAMILIEBARNEHAGE,
    ) = PgiOrdning(
            skatteordning = skatteordning,
            datoForFastsetting = datoForFastSetting,
            pgiLoenn = pgiLoenn,
            pgiLoennPensjonsdel = pgiLoennPensjonsdel,
            pgiNaering = pgiNaering,
            pgiNaeringFiskeFangstFamiliebarnehage = pgiNaeringFiskeFangstFamiliebarnehage
    )
}

private fun String.toLagrePgiRequest() = mapper.readValue<LagrePgiRequest>(this)
private fun List<PgiOrdning>.getSkatteordning(ordning: String) = find { it.skatteordning == ordning }
