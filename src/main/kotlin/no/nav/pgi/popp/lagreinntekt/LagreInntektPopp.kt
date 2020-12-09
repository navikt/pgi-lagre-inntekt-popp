package no.nav.pgi.popp.lagreinntekt

import no.nav.pensjon.samhandling.env.getVal
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
import kotlin.system.exitProcess

internal class LagreInntektPopp(kafkaFactory: KafkaFactory = KafkaInntektFactory(), env: Map<String, String> = System.getenv()) {
    private var pgiConsumer = PensjonsgivendeInntektConsumer(kafkaFactory)
    private var hendelseProducer = HendelseProducer(kafkaFactory)
    private val poppClient = PoppClient(env.getVal("POPP_URL"))
    private var closed: AtomicBoolean = AtomicBoolean(false)

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
        } catch (e: TopicAuthorizationException) {
            refreshKafkaCredentials()
        } catch (e: Exception) {
            LOG.error(e.message)
            e.printStackTrace()
            exitProcess(1)
        } while (loopForever && !closed.get())
    }

    internal fun close() {
        closed.set(true)
        pgiConsumer.close()
        hendelseProducer.close()
    }

    // TODO close consumer and producer ???
    // TODO thread sleep ??
    private fun refreshKafkaCredentials(kafkaFactory: KafkaFactory = KafkaInntektFactory()) {
        LOG.warn("TopicAuthorizationException recieved. Attempting credential rotation")
        pgiConsumer = PensjonsgivendeInntektConsumer(kafkaFactory)
        hendelseProducer = HendelseProducer(kafkaFactory)
        Thread.sleep(5000)
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