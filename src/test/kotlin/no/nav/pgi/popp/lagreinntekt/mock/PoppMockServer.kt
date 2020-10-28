package no.nav.pgi.popp.lagreinntekt.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson

private const val POPP_PORT = 1080
private const val POPP_PATH = "/pgi/lagreinntekt"

internal class PoppMockServer {
    private val poppApiMockServer = WireMockServer(POPP_PORT)

    init {
        poppApiMockServer.start()
        mockHttpResponseFromPopp("1000", "2018", WireMock.created())

        mockHttpResponseFromPopp("2222", "2018", WireMock.serverError())

        mockHttpResponseFromPopp("3000", "2018", WireMock.created())
        mockHttpResponseFromPopp("3333", "2018", WireMock.serverError())

        mockHttpResponseFromPopp("4000", "2018", WireMock.created())
        mockHttpResponseFromPopp("4444", "2018", WireMock.serverError())

        mockHttpResponseFromPopp("5555", "2018", WireMock.serverError())
        mockHttpResponseFromPopp("5556", "2018", WireMock.serverError())
    }

    internal fun stop() {
        poppApiMockServer.stop()
    }


    private fun mockHttpResponseFromPopp(identifikator: String, aar: String, responseCode: ResponseDefinitionBuilder) {
        val requestBodyJson = "\"{\\\"identifikator\\\": \\\"${identifikator}\\\", \\\"inntektsAar\\\": \\\"$aar\\\"}\""
        poppApiMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo(POPP_PATH))
                        .withRequestBody(
                                equalToJson(requestBodyJson, false, false))
                        .willReturn(responseCode))
    }
}