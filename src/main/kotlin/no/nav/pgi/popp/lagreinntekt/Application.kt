package no.nav.pgi.popp.lagreinntekt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.pensjon.samhandling.env.getVal
import no.nav.pensjon.samhandling.liveness.isAlive
import no.nav.pensjon.samhandling.liveness.isReady
import no.nav.pensjon.samhandling.metrics.metrics
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_TOPIC
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess


fun main() {
    val application = Application()
    application.storePensjonsgivendeInntekterInPopp()
}

internal class Application(private val kafkaConfig: KafkaConfig = KafkaConfig()) {
    private val server = embeddedServer(Netty, createApplicationEnvironment())

    init {
        server.addShutdownHook { stopServer() }
        server.start(wait = false)

    }

    private fun createApplicationEnvironment(serverPort: Int = 8080) =
            applicationEngineEnvironment {
                connector { port = serverPort }
                module {
                    isAlive()
                    isReady()
                    metrics()
                }
            }

    internal fun storePensjonsgivendeInntekterInPopp(env: Map<String, String> = System.getenv(), loopForever: Boolean = true) {
        val consumer = PensjonsgivendeInntektConsumer(kafkaConfig)
        val producer = HendelseProducer(kafkaConfig)
        val poppClient = PoppClient(env.getVal("POPP_URL"))

        do try {
            val inntekter = consumer.getInntekter()
            log.debug("Inntekter polled from topic: {$inntekter.size}")

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

    private fun stopServer() {
        server.stop(100, 100)
    }

    companion object {
        private val log = LoggerFactory.getLogger(Application::class.java)
    }
}
