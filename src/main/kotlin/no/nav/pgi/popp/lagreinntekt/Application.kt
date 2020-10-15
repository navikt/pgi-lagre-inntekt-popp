package no.nav.pgi.popp.lagreinntekt

import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*


fun main() {
    createApplication().start(wait = false)
}

internal fun createApplication(serverPort: Int = 8080, kafkaConfig: KafkaConfig = KafkaConfig()) =
        embeddedServer(Netty, createApplicationEnvironment(serverPort, kafkaConfig))

private fun createApplicationEnvironment(serverPort: Int, kafkaConfig: KafkaConfig) =
        applicationEngineEnvironment {
            connector { port = serverPort }
            module {
                liveness()
                connectAndConsumeFromKafka(kafkaConfig)
            }
        }

internal fun Application.connectAndConsumeFromKafka(kafkaConfig: KafkaConfig) {
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
