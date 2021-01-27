package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.pensjon.samhandling.env.getVal
import no.nav.pgi.popp.lagreinntekt.popp.LagrePgiRequestMapper.toLagrePgiRequest
import no.nav.pgi.popp.lagreinntekt.popp.token.AadTokenClient
import no.nav.pgi.popp.lagreinntekt.popp.token.AadTokenClient.AadToken
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

internal const val PGI_PATH = "/popp/api/inntekt/pgi"

internal class PoppClient(environment: Map<String, String>, private val tokenProvider: TokenProvider = AadTokenClient(environment)) {
    private val httpClient: HttpClient = HttpClient.newHttpClient()
    private val poppUrl = environment.getVal(POPP_URL) + PGI_PATH

    internal fun postPensjonsgivendeInntekt(pgi: PensjonsgivendeInntekt) =
        httpClient.send(createPostRequest(poppUrl, toLagrePgiRequest(pgi)), HttpResponse.BodyHandlers.ofString())

    private fun createPostRequest(poppUrl: String, lagrePgiRequest: LagrePgiRequest) = HttpRequest.newBuilder()
        .uri(URI.create(poppUrl))
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

