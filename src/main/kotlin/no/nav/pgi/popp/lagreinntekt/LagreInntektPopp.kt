package no.nav.pgi.popp.lagreinntekt

import no.nav.pensjon.samhandling.maskfnr.maskFnr
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaInntektFactory
import no.nav.pgi.popp.lagreinntekt.kafka.inntekt.PensjonsgivendeInntektConsumer
import no.nav.pgi.popp.lagreinntekt.kafka.republish.HendelseProducer
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.slf4j.LoggerFactory
import java.net.http.HttpResponse
import java.util.concurrent.atomic.AtomicBoolean

internal class LagreInntektPopp(private val poppClient: PoppClient, kafkaFactory: KafkaFactory = KafkaInntektFactory()) {
    private val pgiConsumer = PensjonsgivendeInntektConsumer(kafkaFactory)
    private val hendelseProducer = HendelseProducer(kafkaFactory)

    private var stop: AtomicBoolean = AtomicBoolean(false)

    internal fun start(loopForever: Boolean = true) {
        do try {
            val inntektRecords: List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>> = pgiConsumer.pollInntektRecords()
            //val delayRequestsToPopp = inntektRecords.isDuplicates()
            inntektRecords.forEach { inntektRecord ->
                //if (delayRequestsToPopp) Thread.sleep(50L)
                val response = poppClient.postPensjonsgivendeInntekt(inntektRecord.value())
                when (response.statusCode()) {
                    200 -> {
                        logSuccessfulRequestToPopp(response.statusCode(), inntektRecord.value())
                    }
                    409 -> {
                        logRepublishingFailedInntekt(inntektRecord, response)
                        hendelseProducer.republishHendelse(inntektRecord.key())
                    }
                    else -> {
                        logShuttingDownDueToUnhandledStatus(inntektRecord, response)
                        throw UnhandledStatusCodeFromPoppException(response)
                    }
                }
            }
            pgiConsumer.commit()
        } catch (topicAuthorizationException: TopicAuthorizationException) {
            LOG.warn("Kafka credential rotation triggered. Shutting down app")
            throw topicAuthorizationException
        } while (loopForever && !stop.get())
    }

    internal fun stop() {
        LOG.info("stopping LagreInntektPopp")
        stop.set(true)
    }

    internal fun isClosed() = pgiConsumer.isClosed() && hendelseProducer.isClosed()

    internal fun closeKafka() {
        pgiConsumer.close()
        hendelseProducer.close()
    }

    private fun logSuccessfulRequestToPopp(statusCode: Int, pensjonsgivendeInntekt: PensjonsgivendeInntekt) =
        LOG.info("Successfully added inntekt to POPP PoppResponse(Status $statusCode) for inntekt: ${pensjonsgivendeInntekt.toString().maskFnr()}")

    private fun logRepublishingFailedInntekt(inntektRecord: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>?, response: HttpResponse<String>) =
        LOG.warn(("Failed to add inntekt to POPP initiating resending PoppResponse(Status: ${response.statusCode()}. Body: ${response.body()}) $inntektRecord").maskFnr())

    private fun logShuttingDownDueToUnhandledStatus(inntektRecord: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>?, response: HttpResponse<String>) =
        LOG.error(("Failed to add inntekt to POPP initiating shutdown PoppResponse(Status: ${response.statusCode()}. Body: ${response.body()}) $inntektRecord ").maskFnr())

    private companion object {
        private val LOG = LoggerFactory.getLogger(LagreInntektPopp::class.java)
    }
}

internal class UnhandledStatusCodeFromPoppException(response: HttpResponse<String>) :
    Exception("""Unhandled status code in PoppResponse(Status: ${response.statusCode()}. Body: ${response.body()})""".maskFnr())

private fun List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>.isDuplicates() =
    map { it.key() }.toHashSet().size == size