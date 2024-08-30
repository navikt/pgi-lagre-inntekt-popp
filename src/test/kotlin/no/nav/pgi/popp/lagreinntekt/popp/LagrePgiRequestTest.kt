package no.nav.pgi.popp.lagreinntekt.popp

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.pgi.popp.lagreinntekt.popp.InntektType.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


private val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LagrePgiRequestTest {
    private val personIdentifikator = "12345678901"
    private val inntektsAar = 2020
    private val datoForFastsetting = "2020-01-01"
    private val belop = 1L
    private val sekvensnummer = 5L

    @Test
    fun `map LagrePgiRequest to json string`() {
        val expectedJson = """
            {
                "personIdentifikator": "$personIdentifikator",
                "inntektsaar": $inntektsAar,
                "sekvensnummer": $sekvensnummer,
                "pgiList": [
                    {
                        "inntektType": "FL_PGI_LOENN",
                        "datoForFastsetting": "$datoForFastsetting",
                        "belop": $belop
                    },
                    {
                        "inntektType": "FL_PGI_NAERING",
                        "datoForFastsetting": "$datoForFastsetting",
                        "belop": $belop
                    },
                    {
                        "inntektType": "FL_PGI_LOENN_PD",
                        "datoForFastsetting": "$datoForFastsetting",
                        "belop": $belop
                    },
                    {
                        "inntektType": "FL_PGI_NAERING_FFF",
                        "datoForFastsetting": "$datoForFastsetting",
                        "belop": $belop
                    }
                ]
            }
        """

        val jsonRequest = createLagrePgiRequest(
            pgiList = listOf(
                createPgi(FL_PGI_LOENN),
                createPgi(FL_PGI_NAERING),
                createPgi(FL_PGI_LOENN_PD),
                createPgi(FL_PGI_NAERING_FFF),
            )
        ).toJson()

        assertEquals(mapper.readTree(expectedJson), mapper.readTree(jsonRequest))
    }

    @Test
    fun `map null value to null`() {
        val expectedJson = """
            {
                "personIdentifikator": "$personIdentifikator",
                "inntektsaar": $inntektsAar,
                "sekvensnummer": $sekvensnummer,
                "pgiList": [
                    {
                        "inntektType": "FL_PGI_LOENN",
                        "datoForFastsetting": "$datoForFastsetting",
                        "belop": null
                    }
                ]
            }
        """

        val jsonRequest = createLagrePgiRequest(pgiList = listOf(createPgi(inntektType = FL_PGI_LOENN, pgiBelop = null)))
            .toJson()

        assertEquals(mapper.readTree(expectedJson), mapper.readTree(jsonRequest))
    }

    private fun createLagrePgiRequest(
        identifikator: String = personIdentifikator,
        aar: Int = inntektsAar,
        pgiList: List<Pgi> = listOf(createPgi()),
        sekvensnr: Long = sekvensnummer
    ) = LagrePgiRequest(personIdentifikator = identifikator, inntektsaar = aar, pgiList = pgiList, sekvensnummer = sekvensnr)

    private fun createPgi(
        inntektType: InntektType = FL_PGI_LOENN, datoForFastSetting: String = datoForFastsetting, pgiBelop: Long? = belop,
    ) = Pgi(inntektType = inntektType, datoForFastsetting = datoForFastSetting, belop = pgiBelop)


}



