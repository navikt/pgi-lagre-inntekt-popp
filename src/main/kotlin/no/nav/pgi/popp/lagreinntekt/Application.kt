package no.nav.pgi.popp.lagreinntekt

import no.nav.pensjon.samhandling.naisserver.naisServer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaInntektFactory

fun main() {
    val application = Application()
    try {
        application.startLagreInntektPopp()
    } catch (e: Exception) {
        application.close()
    }
}

internal class Application(kafkaFactory: KafkaFactory = KafkaInntektFactory(), env: Map<String, String> = System.getenv()) {
    private val naisServer = naisServer()
    private val lagreInntektPopp = LagreInntektPopp(kafkaFactory, env)

    init {
        addShutdownHook()
        naisServer.start()
    }

    internal fun startLagreInntektPopp(loopForever: Boolean = true) {
        lagreInntektPopp.start(loopForever)
    }

    internal fun close() {
        naisServer.stop(0, 0)
        lagreInntektPopp.close()
    }

    private fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                close()
            } catch (e: Exception) {
                //TODO håndter
            }
        })
    }
}

