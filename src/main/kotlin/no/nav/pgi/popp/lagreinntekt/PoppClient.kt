package no.nav.pgi.popp.lagreinntekt

import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

internal class PoppClient(private val url: String) {
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    internal fun <T> send(httpRequest: HttpRequest, responseBodyHandler: HttpResponse.BodyHandler<T>): HttpResponse<T> =
            httpClient.send(httpRequest, responseBodyHandler)

    internal fun storePensjonsgivendeInntekter(inntekt : ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>): HttpResponse<String> {
        val pensjonsgivendeInntekt = inntekt.value()
        val request = createPostRequest(url, pensjonsgivendeInntekt.toJson())
        return this.send(request, HttpResponse.BodyHandlers.ofString())

    }
}

internal fun createPostRequest(url: String, body: String) = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()

