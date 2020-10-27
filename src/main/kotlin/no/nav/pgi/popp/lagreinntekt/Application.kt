package no.nav.pgi.popp.lagreinntekt

import no.nav.pensjon.samhandling.env.getVal
import no.nav.pensjon.samhandling.naisserver.naisServer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_TOPIC
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

fun main() {
    val naisServer = naisServer().start()
    try {
        Application().storePensjonsgivendeInntekterInPopp()
    } catch (e: Exception) {
        naisServer.stop(100,100)
    }
}

internal class Application(kafkaConfig: KafkaConfig = KafkaConfig(),
                           env: Map<String, String> = System.getenv()) {
    private val consumer = PensjonsgivendeInntektConsumer(kafkaConfig)
    private val producerRepubliserHendelser = HendelseProducer(kafkaConfig)
    private val poppClient = PoppClient(env.getVal("POPP_URL"))

    internal fun storePensjonsgivendeInntekterInPopp(loopForever: Boolean = true) {

        do try {
            consumer.getInntekter()
                    .also { consumerRecords ->
                        log.debug("Antall ConsumerRecords polled from topic $PGI_INNTEKT_TOPIC: ${consumerRecords.size}")
                        consumerRecords.forEach{ log.debug("key ${it.key()} - value ${it.value()}")}
                    }
                    .let { lagrePensjonsgivendeInntekterTilPopp(it) }
                    .also {
                        republiserHendelser(it) }
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

    private fun republiserHendelser(inntekterFeiletTilPopp: MutableList<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>) {
        inntekterFeiletTilPopp.forEach {
            producerRepubliserHendelser.rePublishHendelse(it.key())
        }.also { log.warn("Republiserer ${inntekterFeiletTilPopp.size} hendelse(r) til topic $PGI_HENDELSE_TOPIC.") }
    }

    private fun lagrePensjonsgivendeInntekterTilPopp(inntekter: List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>): MutableList<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>> {
        val inntekterFeiletTilPopp = mutableListOf<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>()
        inntekter.forEach { inntekt ->
            val response = poppClient.storePensjonsgivendeInntekter(inntekt)
            if (response.statusCode() != 201) {
                log.warn("Feil ved lagring av inntekt til POPP.")
                inntekterFeiletTilPopp.add(inntekt)

            }
        }
        return inntekterFeiletTilPopp
    }

    companion object {
        private val log = LoggerFactory.getLogger(Application::class.java)
    }
}
