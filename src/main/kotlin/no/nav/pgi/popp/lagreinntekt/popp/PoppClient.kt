package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.pgi.popp.lagreinntekt.toJson
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.jvm.Throws

class PoppClient(private val url: String) {
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    @Throws(PoppClientIOException::class)
    fun savePensjonsgivendeInntekter(inntekter: List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>): MutableList<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>> {
        log.debug("savePensjonsgivendeInntekter now")
        val rePublishToHendelse = mutableListOf<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>()
        try {
            inntekter.forEach { inntekt ->
                val response = httpPostPensjonsgivendeInntekt(inntekt)
                if (response.statusCode() != 201) {
                    log.warn("Feil ved lagring av inntekt til POPP.")
                    rePublishToHendelse.add(inntekt)

                }
            }
        } catch (e: IOException) {
            log.warn("Feil ved lagring av pensjonsgivende inntekt til POPP.", e)
            throw PoppClientIOException("Feil ved lagring av pensjonsgivende inntekt til POPP.", e)
        }
        return rePublishToHendelse
    }

    private fun httpPostPensjonsgivendeInntekt(inntekt: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>): HttpResponse<String> {
        val pensjonsgivendeInntekt = inntekt.value()
        val request = createPostRequest(url, pensjonsgivendeInntekt.toJson())
        return this.send(request, HttpResponse.BodyHandlers.ofString())

    }

    private fun <T> send(httpRequest: HttpRequest, responseBodyHandler: HttpResponse.BodyHandler<T>): HttpResponse<T> =
            httpClient.send(httpRequest, responseBodyHandler)

    companion object {
        private val log = LoggerFactory.getLogger(PoppClient::class.java)
    }
}

internal fun createPostRequest(url: String, body: String) = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()

class PoppClientIOException(override val message: String?, override val cause: Throwable?) : IOException(message, cause)
