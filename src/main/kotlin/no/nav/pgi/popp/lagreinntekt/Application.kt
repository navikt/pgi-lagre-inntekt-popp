package no.nav.pgi.popp.lagreinntekt

import no.nav.pensjon.samhandling.env.getVal
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_TOPIC
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess


fun main() {
    ReadinessServer.start()
    Application().storePensjonsgivendeInntekterInPopp()
}

internal class Application(private val kafkaConfig: KafkaConfig = KafkaConfig()) {

    internal fun storePensjonsgivendeInntekterInPopp(env: Map<String, String> = System.getenv(), loopForever: Boolean = true) {
        val consumer = PensjonsgivendeInntektConsumer(kafkaConfig)
        val producer = HendelseProducer(kafkaConfig)
        val poppClient = PoppClient(env.getVal("POPP_URL"))

        do try {
            val inntekter = consumer.getInntekter()
            log.debug("Antall ConsumerRecords polled from topic: ${inntekter.size}")

            inntekter.forEach { inntekt ->
                val response = poppClient.storePensjonsgivendeInntekter(inntekt)
                if (response.statusCode() != 200) {
                    log.warn("Feil ved lagring av inntekt til POPP. Republiserer hendelse til topic $PGI_HENDELSE_TOPIC")
                    producer.rePublishHendelse(inntekt.key())
                }
            }

        } catch (e: Exception) {
            log.error(e.message)
            e.printStackTrace()
            exitProcess(1)
        } while (loopForever)
    }

    companion object {
        private val log = LoggerFactory.getLogger(Application::class.java)
    }
}
