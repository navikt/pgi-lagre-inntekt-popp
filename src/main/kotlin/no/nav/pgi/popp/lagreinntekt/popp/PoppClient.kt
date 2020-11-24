package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

internal const val PGI_PATH = "/popp-ws/api/inntekt/pgi"

internal class PoppClient(private val url: String) {
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    internal fun postPensjonsgivendeInntekt(pensjonsgivendeInntekt: PensjonsgivendeInntekt) =
            httpClient.send(createPostRequest(url, pensjonsgivendeInntekt.toString()), HttpResponse.BodyHandlers.ofString())

    private fun createPostRequest(url: String, body: String) = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer dummyvalue") //TODO: Proper token
            .header("Content-Type", "application/json")
            .header("Nav-Call-Id", UUID.randomUUID().toString())
            .header("Nav-Consumer-Id", "pgi-lagre-inntekt-popp")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
}

