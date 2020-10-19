package no.nav.pgi.popp.lagreinntekt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.pensjon.samhandling.env.getVal
import no.nav.pensjon.samhandling.liveness.isAlive
import no.nav.pensjon.samhandling.liveness.isReady
import no.nav.pensjon.samhandling.metrics.metrics
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
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
            println("Inntekter polled from topic: {$inntekter.size}")
            inntekter.forEach { inntekt ->
                val response = poppClient.storePensjonsgivendeInntekter(inntekt)
                if (response.statusCode() != 200) {
                    println("Feil i lagring to POPP. Republish ")
                    producer.rePublishHendelse(inntekt.key())
                }
            }

        } catch (e: Exception) {
            println(e.message)
            e.printStackTrace()
            exitProcess(1)
        } while (loopForever)
    }

    internal fun stopServer() {
        server.stop(100, 100)
    }
}
