package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.pensjon.samhandling.env.getVal
import no.nav.pensjon.samhandling.maskfnr.maskFnr
import no.nav.pgi.popp.lagreinntekt.LagreInntektPopp
import no.nav.pgi.popp.lagreinntekt.popp.LagrePgiRequestMapper.toLagrePgiRequest
import no.nav.pgi.popp.lagreinntekt.popp.token.AadTokenClient
import no.nav.pgi.popp.lagreinntekt.popp.token.AadTokenClient.AadToken
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import javax.ws.rs.core.UriBuilder

internal const val PGI_PATH = "/popp/api/inntekt/pgi"
private val LOG = LoggerFactory.getLogger(LagreInntektPopp::class.java)

internal class PoppClient(environment: Map<String, String>, private val tokenProvider: TokenProvider = AadTokenClient(environment)) {
    private val httpClient: HttpClient = HttpClient.newHttpClient()
    private val poppHost = environment.getVal(POPP_URL)
    private val poppUrl = UriBuilder.fromPath(poppHost).path(PGI_PATH).build()

    internal fun postPensjonsgivendeInntekt(pgi: PensjonsgivendeInntekt): HttpResponse<String> {
        val lagrePgiRequest = toLagrePgiRequest(pgi)
        LOG.info(lagrePgiRequest.toJson().maskFnr()) //TODO: Make it pretty
        return httpClient.send(createPostRequest(poppUrl, lagrePgiRequest), HttpResponse.BodyHandlers.ofString())
    }

    private fun createPostRequest(poppUrl: URI, lagrePgiRequest: LagrePgiRequest) = HttpRequest.newBuilder()
        .uri(poppUrl)
        .header("Authorization", "Bearer ${tokenProvider.getToken().accessToken}")
        .header("Content-Type", "application/json")
        .header("Nav-Call-Id", UUID.randomUUID().toString())
        .header("Nav-Consumer-Id", "pgi-lagre-inntekt-popp")
        .POST(HttpRequest.BodyPublishers.ofString(lagrePgiRequest.toJson()))
        .build()

    internal interface TokenProvider {
        fun getToken(): AadToken
    }

    private companion object EnvironmentKey {
        private const val POPP_URL = "POPP_URL"
    }
}

