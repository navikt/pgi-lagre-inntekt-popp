package no.nav.pgi.popp.lagreinntekt.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing
import no.nav.pgi.popp.lagreinntekt.popp.PGI_PATH

private const val POPP_PORT = 31241
internal const val POPP_MOCK_URL = "http://localhost:$POPP_PORT$PGI_PATH"

internal class PoppMockServer {
    private val poppApiMockServer = WireMockServer(POPP_PORT)

    companion object {
        const val FNR_NR1_201 = "11111111201"
        const val FNR_NR2_201 = "22222222201"
        const val FNR_NR3_201 = "33333333201"

        const val FNR_NR1_500 = "11111111111"
        const val FNR_NR2_500 = "22222222222"
        const val FNR_NR3_500 = "33333333333"
        const val FNR_NR4_500 = "44444444444"
        const val FNR_NR5_500 = "55555555555"
    }

    init {
        poppApiMockServer.start()

        mockResponseFromPopp(FNR_NR2_500, WireMock.serverError())
        mockResponseFromPopp(FNR_NR1_500, WireMock.serverError())
        mockResponseFromPopp(FNR_NR3_500, WireMock.serverError())
        mockResponseFromPopp(FNR_NR4_500, WireMock.serverError())
        mockResponseFromPopp(FNR_NR5_500, WireMock.serverError())

        mockResponseFromPopp(FNR_NR1_201, WireMock.created())
        mockResponseFromPopp(FNR_NR2_201, WireMock.created())
        mockResponseFromPopp(FNR_NR3_201, WireMock.created())
    }

    internal fun stop() {
        poppApiMockServer.stop()
    }

    private fun mockResponseFromPopp(identifikator: String, responseCode: ResponseDefinitionBuilder) {
        poppApiMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo(PGI_PATH))
                        .withRequestBody(containing(identifikator))
                        .willReturn(responseCode))
    }
}