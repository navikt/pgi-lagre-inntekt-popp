package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.pgi.popp.lagreinntekt.toJson
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

// TODO gjør mapping til dto inne i denne klassen
class PoppClient(private val url: String) {
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    internal fun postPensjonsgivendeInntekt(pensjonsgivendeInntektDto: PensjonsgivendeInntektDto): HttpResponse<String> =
            httpClient.send(createPostRequest(url, pensjonsgivendeInntektDto.toJson()), HttpResponse.BodyHandlers.ofString())
}

private fun createPostRequest(url: String, body: String) = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()

