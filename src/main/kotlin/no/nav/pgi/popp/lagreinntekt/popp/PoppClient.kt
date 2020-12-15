package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.pensjon.samhandling.env.getVal
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
    private val poppUrl = environment.getVal(POPP_URL)

    internal fun postPensjonsgivendeInntekt(pensjonsgivendeInntekt: PensjonsgivendeInntekt) =
        httpClient.send(createPostRequest(poppUrl, pensjonsgivendeInntekt.toString()), HttpResponse.BodyHandlers.ofString())

    private fun createPostRequest(url: String, body: String) = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Authorization", "Bearer ${tokenProvider.getToken().accessToken}")
        .header("Content-Type", "application/json")
        .header("Nav-Call-Id", UUID.randomUUID().toString())
        .header("Nav-Consumer-Id", "pgi-lagre-inntekt-popp")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()

    internal interface TokenProvider {
        fun getToken(): AadToken
    }

    private companion object EnvironmentKey {
        private const val POPP_URL = "POPP_URL"
    }
}

