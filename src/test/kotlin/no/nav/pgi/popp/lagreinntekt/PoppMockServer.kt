package no.nav.pgi.popp.lagreinntekt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson

private const val POPP_PORT = 1080
private const val POPP_PATH = "/pgi/lagreinntekt"
private const val POPP_URL = "http://localhost:$POPP_PORT$POPP_PATH"

internal class PoppMockServer {
    private val poppApiMockServer = WireMockServer(POPP_PORT)

    init {
        poppApiMockServer.start()
        mockHttpResponse500FromPopp()
        mockHttpResponse200FromPopp()
    }

    internal fun stop() {
        poppApiMockServer.stop()
    }


    private fun mockHttpResponse500FromPopp() {
        val requestBodyJson = "\"{\\\"identifikator\\\": \\\"2345\\\", \\\"inntektsAar\\\": \\\"2018\\\"}\""
        poppApiMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo(POPP_PATH))
                        .withRequestBody(
                                equalToJson(requestBodyJson, false, false))
                        .willReturn(WireMock.serverError()))
    }

    private fun mockHttpResponse200FromPopp() {
        val requestBodyJson = "\"{\\\"identifikator\\\": \\\"1234\\\", \\\"inntektsAar\\\": \\\"2018\\\"}\""
        poppApiMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo(POPP_PATH))
                        .withRequestBody(
                                equalToJson(requestBodyJson,false, false))
                        .willReturn(WireMock.ok()))
    }
}