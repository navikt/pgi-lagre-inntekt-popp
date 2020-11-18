package no.nav.pgi.popp.lagreinntekt.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing

private const val POPP_PORT = 1080
private const val POPP_PATH = "/pgi/lagreinntekt"
internal const val POPP_MOCK_URL = "http://localhost:$POPP_PORT$POPP_PATH"

internal class PoppMockServer {
    private val poppApiMockServer = WireMockServer(POPP_PORT)

    companion object {
        const val FNR_NR1_500 = "11111111111"

        const val FNR_NR1_201 = "11111111201"

        const val FNR_NR2_500 = "22222222222"
        const val FNR_NR2_201 = "22222222201"

        const val FNR_NR3_500 = "33333333333"
        const val FNR_NR3_201 = "44444444201"

        const val FNR_NR4_500 = "44444444444"
        const val FNR_NR5_500 = "55555555555"
    }

    init {
        poppApiMockServer.start()

        mockHttpResponseFromPopp(FNR_NR2_500, "2018", WireMock.serverError())
        mockHttpResponseFromPopp(FNR_NR1_500, "2018", WireMock.serverError())
        mockHttpResponseFromPopp(FNR_NR3_500, "2018", WireMock.serverError())
        mockHttpResponseFromPopp(FNR_NR4_500, "2018", WireMock.serverError())
        mockHttpResponseFromPopp(FNR_NR5_500, "2018", WireMock.serverError())

        mockHttpResponseFromPopp(FNR_NR1_201, "2018", WireMock.created())
        mockHttpResponseFromPopp(FNR_NR2_201, "2018", WireMock.created())
        mockHttpResponseFromPopp(FNR_NR3_201, "2018", WireMock.created())
    }

    internal fun stop() {
        poppApiMockServer.stop()
    }

    //TODO Gjør om pensjonsgivende inntekt Dto til noe fornuftig
    private fun mockHttpResponseFromPopp(identifikator: String, inntektsAar: String, responseCode: ResponseDefinitionBuilder) {
        poppApiMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo(POPP_PATH))
                        .withRequestBody(containing(""""norskPersonidentifikator":"$identifikator""""))
                        .withRequestBody(containing(""""inntektsaar":"$inntektsAar""""))
                        .withRequestBody(containing("pensjonsgivendeInntekt"))
                        .willReturn(responseCode))
    }
}