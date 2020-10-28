package no.nav.pgi.popp.lagreinntekt

import no.nav.pensjon.samhandling.env.getVal
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

internal class PgiLagreInntektPopp(kafkaConfig: KafkaConfig = KafkaConfig(), env: Map<String, String> = System.getenv()) {
    private val consumer = PensjonsgivendeInntektConsumer(kafkaConfig)
    private val producerRepubliserHendelser = HendelseProducer(kafkaConfig)
    private val poppClient = PoppClient(env.getVal("POPP_URL"))

    internal fun start(loopForever: Boolean = true) {

        do try {
            consumer.getInntekter()
                    .also { consumerRecords ->
                        log.debug("Antall ConsumerRecords polled from topic $PGI_INNTEKT_TOPIC: ${consumerRecords.size}")
                        consumerRecords.forEach { log.debug("key ${it.key()} - value ${it.value()}") }
                    }
                    .let { poppClient.savePensjonsgivendeInntekter(it) }
                    .also {
                        producerRepubliserHendelser.rePublishHendelser(it)
                    }
                    .also {
                        log.debug("Commit consumer")
                        consumer.commit()
                    }

        } catch (e: Exception) {
            log.error(e.message)
            e.printStackTrace()
            exitProcess(1)
        } while (loopForever)
    }

    companion object {
        private val log = LoggerFactory.getLogger(PgiLagreInntektPopp::class.java)
    }
}