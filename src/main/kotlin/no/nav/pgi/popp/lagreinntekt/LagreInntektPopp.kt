package no.nav.pgi.popp.lagreinntekt

import no.nav.pensjon.samhandling.env.getVal
import no.nav.pensjon.samhandling.maskfnr.maskFnr
import no.nav.pgi.popp.lagreinntekt.kafka.republish.HendelseProducer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.PensjonsgivendeInntektConsumer
import no.nav.pgi.popp.lagreinntekt.kafka.inntekt.PensjonsgivendeInntektConsumer
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.http.HttpResponse
import kotlin.system.exitProcess

internal class LagreInntektPopp(kafkaConfig: KafkaConfig = KafkaConfig(), env: Map<String, String> = System.getenv()) {
    private var consumer = PensjonsgivendeInntektConsumer(kafkaConfig)
    private var hendelseProducer = HendelseProducer(kafkaConfig)
    private val poppClient = PoppClient(env.getVal("POPP_URL"))

    internal fun start(loopForever: Boolean = true) {
        do try {
            val inntektRecords = consumer.pollInntektRecords()
            inntektRecords.forEach { inntektRecord ->

                val response = poppClient.postPensjonsgivendeInntekt(inntektRecord.value())
                if (response.statusCode() != 201) {
                    logFailedInntektToPopp(inntektRecord, response)
                    hendelseProducer.republishHendelse(inntektRecord.key())
                }
            }
            consumer.commit()
        } catch (e: TopicAuthorizationException) {
            LOG.warn("")
            val kafkaConfig = KafkaConfig()
            consumer = PensjonsgivendeInntektConsumer(kafkaConfig)
            hendelseProducer = HendelseProducer(kafkaConfig)
        } catch (e: Exception) {
            LOG.error(e.message)
            e.printStackTrace()
            exitProcess(1)
        } while (loopForever)
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