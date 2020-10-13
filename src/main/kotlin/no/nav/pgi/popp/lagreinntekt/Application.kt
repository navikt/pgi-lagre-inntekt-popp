package no.nav.pgi.popp.lagreinntekt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.pensjon.samhandling.liveness.isAlive
import no.nav.pensjon.samhandling.liveness.isReady
import no.nav.pensjon.samhandling.metrics.metrics


fun main() {
    createApplication().start(wait = false)
}

internal fun createApplication(serverPort: Int = 8080, kafkaConfig: KafkaConfig = KafkaConfig()) =
        embeddedServer(Netty, createApplicationEnvironment(serverPort, kafkaConfig))

private fun createApplicationEnvironment(serverPort: Int, kafkaConfig: KafkaConfig) =
        applicationEngineEnvironment {
            connector { port = serverPort }
            module {
                isAlive()
                isReady()
                metrics()
                connectAndConsumeFromKafka(kafkaConfig)
            }
        }

internal fun connectAndConsumeFromKafka(kafkaConfig: KafkaConfig) {
    val consumer = PensjonsgivendeInntektConsumer(kafkaConfig)
    while (true) {
        val inntekter = consumer.getInntekter()
        println("Inntekter fetched: $inntekter.size")
        Thread.sleep(5000)
    }
}
