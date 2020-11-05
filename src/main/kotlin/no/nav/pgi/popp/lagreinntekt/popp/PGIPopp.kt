package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.io.IOException

internal class PoppClientIOException(override val message: String?, override val cause: Throwable?) : IOException(message, cause)

private val LOGGER = LoggerFactory.getLogger(PGIPopp::class.java)

internal class PGIPopp(url: String) {
    val poppClient: PoppClient = PoppClient(url)

    @Throws(PoppClientIOException::class)
    fun savePensjonsgivendeInntekter(inntekter: List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>): MutableList<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>> {
        LOGGER.debug("savePensjonsgivendeInntekter now")
        val rePublishToHendelse = mutableListOf<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>()
        try {
            inntekter.forEach { inntektRecord: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt> ->
                val response = poppClient.postPensjonsgivendeInntekt(mapToPensjonsgivendeInntektDto(inntektRecord))
                if (response.statusCode() != 201) {
                    LOGGER.warn("Feil ved lagring av inntekt til POPP.")
                    rePublishToHendelse.add(inntektRecord)

                }
            }
        } catch (e: IOException) {
            LOGGER.warn("Feil ved lagring av pensjonsgivende inntekt til POPP.", e)
            throw PoppClientIOException("Feil ved lagring av pensjonsgivende inntekt til POPP.", e)
        }
        return rePublishToHendelse
    }
}