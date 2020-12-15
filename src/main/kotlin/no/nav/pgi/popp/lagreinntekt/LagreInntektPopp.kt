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
            val inntektRecords = pgiConsumer.pollInntektRecords()
            inntektRecords.forEach { inntektRecord ->
                val response = poppClient.postPensjonsgivendeInntekt(inntektRecord.value())
                if (response.statusCode() != 201) {
                    logFailedInntektToPopp(inntektRecord, response)
                    hendelseProducer.republishHendelse(inntektRecord.key())
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

    private fun logFailedInntektToPopp(inntektRecord: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>?, response: HttpResponse<String>) {
        LOG.warn(("$inntektRecord could not be sent to popp.\n" +
                    "Status code: ${response.statusCode()}.\n" +
                    "Body: ${response.body()}").maskFnr())
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(LagreInntektPopp::class.java)
    }
}