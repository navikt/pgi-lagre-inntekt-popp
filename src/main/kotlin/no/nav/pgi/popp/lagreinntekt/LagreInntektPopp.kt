package no.nav.pgi.popp.lagreinntekt

import no.nav.pensjon.samhandling.env.getVal
import no.nav.pgi.popp.lagreinntekt.kafka.HendelseProducer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
import no.nav.pgi.popp.lagreinntekt.kafka.PensjonsgivendeInntektConsumer
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import no.nav.pgi.popp.lagreinntekt.popp.PoppClientIOException
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

internal class LagreInntektPopp(kafkaConfig: KafkaConfig = KafkaConfig(), env: Map<String, String> = System.getenv()) {
    private val consumer = PensjonsgivendeInntektConsumer(kafkaConfig)
    private val producerRepubliserHendelser = HendelseProducer(kafkaConfig)
    private val poppClient = PoppClient(env.getVal("POPP_URL"))

    internal fun start(loopForever: Boolean = true) {

        do try {
            val inntekter = consumer.getInntekter()
            logRecordsPolledFromTopic(inntekter)
            inntekter.forEach { log.debug("key ${it.key()} - value ${it.value()}") }
            val leftoverInntekt = poppClient.savePensjonsgivendeInntekter(inntekter)
            producerRepubliserHendelser.rePublishHendelser(leftoverInntekt)
            consumer.commit()

        } catch (poppClientIOException: PoppClientIOException) {
            log.warn(poppClientIOException.message, poppClientIOException)
        } catch (e: Exception) {
            log.error(e.message)
            e.printStackTrace()
            exitProcess(1)
        } while (loopForever)
    }

    private fun logRecordsPolledFromTopic(consumerRecords: List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>) {
        log.debug("Antall ConsumerRecords polled from topic $PGI_INNTEKT_TOPIC: ${consumerRecords.size}")
    }

    companion object {
        private val log = LoggerFactory.getLogger(LagreInntektPopp::class.java)
    }
}