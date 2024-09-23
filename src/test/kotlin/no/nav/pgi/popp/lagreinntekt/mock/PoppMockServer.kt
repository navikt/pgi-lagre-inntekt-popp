package no.nav.pgi.popp.lagreinntekt.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.pgi.popp.lagreinntekt.popp.PGI_PATH

private const val POPP_PORT = 31241
internal const val POPP_MOCK_URL = "http://localhost:$POPP_PORT"

internal class PoppMockServer {
    private val poppApiMockServer = WireMockServer(POPP_PORT)

    companion object {
        const val FNR_NR1_200 = "11111111201"
        const val FNR_NR2_200 = "22222222201"
        const val FNR_NR3_200 = "33333333201"

        const val FNR_NR1_409 = "11111111111"
        const val FNR_NR2_409 = "22222222222"
        const val FNR_NR3_409 = "33333333333"
        const val FNR_NR4_409 = "44444444444"
        const val FNR_NR5_409 = "55555555555"
    }

    init {
        poppApiMockServer.start()

        mockResponseFromPopp(FNR_NR2_409, aResponse().withStatus(409).withBody("Bruker eksisterer ikke i PEN"))
        mockResponseFromPopp(FNR_NR1_409, aResponse().withStatus(409).withBody("Bruker eksisterer ikke i PEN"))
        mockResponseFromPopp(FNR_NR3_409, aResponse().withStatus(409).withBody("Bruker eksisterer ikke i PEN"))
        mockResponseFromPopp(FNR_NR4_409, aResponse().withStatus(409).withBody("Bruker eksisterer ikke i PEN"))
        mockResponseFromPopp(FNR_NR5_409, aResponse().withStatus(409).withBody("Fant ikke person"))

        mockResponseFromPopp(FNR_NR1_200, ok())
        mockResponseFromPopp(FNR_NR2_200, ok())
        mockResponseFromPopp(FNR_NR3_200, ok())
    }

    internal fun stop() = poppApiMockServer.stop()
    internal fun reset() = poppApiMockServer.resetAll()

    internal fun `Mock 200 ok`() {
        poppApiMockServer.stubFor(
            post(urlPathEqualTo(PGI_PATH))
                .atPriority(10)
                .willReturn(ok())
        )
    }

    internal fun `Mock 500 server error`() {
        poppApiMockServer.stubFor(
            post(urlPathEqualTo(PGI_PATH))
                .atPriority(10)
                .willReturn(serverError())
        )
    }

    internal fun `Mock 400 bad request with body`(body: String) {
        poppApiMockServer.stubFor(
            post(urlPathEqualTo(PGI_PATH))
                .atPriority(10)
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withBody(body)
                )
        )
    }

    internal fun `Mock 409 Bruker eksisterer ikke i PEN`() {
        poppApiMockServer.stubFor(
            post(urlPathEqualTo(PGI_PATH))
                .atPriority(10)
                .willReturn(
                    aResponse()
                        .withStatus(409)
                        .withBody("Bruker eksisterer ikke i PEN")
                )
        )
    }

    internal fun `Mock 409 Fant ikke person`() {
        poppApiMockServer.stubFor(
            post(urlPathEqualTo(PGI_PATH))
                .atPriority(10)
                .willReturn(
                    aResponse()
                        .withStatus(409)
                        .withBody("Fant ikke person")
                )
        )
    }

    private fun mockResponseFromPopp(identifikator: String, responseCode: ResponseDefinitionBuilder) {
        poppApiMockServer.stubFor(
            post(urlPathEqualTo(PGI_PATH))
                .atPriority(1)
                .withRequestBody(containing(identifikator))
                .willReturn(responseCode)
        )
    }
}