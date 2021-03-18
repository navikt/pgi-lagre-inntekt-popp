package no.nav.pgi.popp.lagreinntekt

import io.prometheus.client.Counter
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


private val pgiPoppRespnseCounter = Counter.build()
    .name("pgi_lagre_inntekt_popp_response_counter")
    .labelNames("statusCode")
    .help("Count response status codes from popp")
    .register()

internal class LagreInntektPopp(
    private val poppClient: PoppClient,
    kafkaFactory: KafkaFactory = KafkaInntektFactory()
) {
    private val pgiConsumer = PensjonsgivendeInntektConsumer(kafkaFactory)
    private val hendelseProducer = HendelseProducer(kafkaFactory)

    private var stop: AtomicBoolean = AtomicBoolean(false)

    internal fun start(loopForever: Boolean = true) {
        do try {
            val inntektRecords: List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>> =
                pgiConsumer.pollInntektRecords()
            //val delayRequestsToPopp = inntektRecords.isDuplicates()
            inntektRecords.forEach { inntektRecord ->
                //if (delayRequestsToPopp) Thread.sleep(50L)
                val response = poppClient.postPensjonsgivendeInntekt(inntektRecord.value())

                pgiPoppRespnseCounter.labels(response.statusCode().toString()).inc()

                when (response.statusCode()) {
                    200 -> {
                        logSuccessfulRequestToPopp(response.statusCode(), inntektRecord.value())
                    }
                    409 -> {
                        logRepublishingFailedInntekt(response, inntektRecord.value())
                        hendelseProducer.republishHendelse(inntektRecord)
                    }
                    else -> {
                        logShuttingDownDueToUnhandledStatus(response, inntektRecord.value())
                        throw UnhandledStatusCodePoppException(response)
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

    private fun logSuccessfulRequestToPopp(statusCode: Int, pensjonsgivendeInntekt: PensjonsgivendeInntekt) {
        LOG.info("Successfully added inntekt to POPP PoppResponse(Status $statusCode) for inntekt: ${pensjonsgivendeInntekt.toString().maskFnr()}")
    }

    private fun logRepublishingFailedInntekt(response: HttpResponse<String>, pensjonsgivendeInntekt: PensjonsgivendeInntekt) {
        LOG.warn(("Failed to add inntekt to POPP initiating resending PoppResponse(Status: ${response.statusCode()}. Body: ${response.body()}) $pensjonsgivendeInntekt").maskFnr())
    }

    private fun logShuttingDownDueToUnhandledStatus(response: HttpResponse<String>, pensjonsgivendeInntekt: PensjonsgivendeInntekt) {
        LOG.error(("Failed to add inntekt to POPP initiating shutdown PoppResponse(Status: ${response.statusCode()}. Body: ${response.body()}) $pensjonsgivendeInntekt ").maskFnr())
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(LagreInntektPopp::class.java)
    }
}

internal class UnhandledStatusCodePoppException(response: HttpResponse<String>) :
    Exception("""Unhandled status code in PoppResponse(Status: ${response.statusCode()}. Body: ${response.body()})""".maskFnr())

private fun List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>.isDuplicates() = map { it.key() }.toHashSet().size == size