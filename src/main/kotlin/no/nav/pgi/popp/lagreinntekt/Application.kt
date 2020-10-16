package no.nav.pgi.popp.lagreinntekt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.pensjon.samhandling.liveness.isAlive
import no.nav.pensjon.samhandling.liveness.isReady
import no.nav.pensjon.samhandling.metrics.metrics


fun main() {
    val application = Application()
    application.connectAndConsumeFromKafka()
}

internal class Application(private val kafkaConfig: KafkaConfig = KafkaConfig()) {

    init {
        val server = embeddedServer(Netty, createApplicationEnvironment())
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

    internal fun connectAndConsumeFromKafka() {
        val consumer = PensjonsgivendeInntektConsumer(kafkaConfig)
        while (true) {
            try {
                val inntekter = consumer.getInntekter()
                println("Inntekter fetched: ${inntekter.size}")
                Thread.sleep(1000)
            } catch (e: Throwable) {
                println(e.message)
                e.printStackTrace()
            }
        }
    }
}
