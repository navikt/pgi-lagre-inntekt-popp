package no.nav.pgi.popp.lagreinntekt

import no.nav.pensjon.samhandling.naisserver.naisServer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import org.slf4j.LoggerFactory

fun main() {
    val application = Application()
    try {
        application.startLagreInntektPopp()
    } catch (e: Exception) {
        application.stop()
    }
}

internal class Application(kafkaConfig: KafkaConfig = KafkaConfig(), env: Map<String, String> = System.getenv()) {
    private val naisServer = naisServer()
    private val lagreInntektPopp = LagreInntektPopp(kafkaConfig, env)

    init {
        naisServer.start()
    }

    internal fun startLagreInntektPopp(loopForever: Boolean = true) {
        lagreInntektPopp.start(loopForever)
    }

    internal fun stop() {
        naisServer.stop(0, 0)
    }

    companion object {
        private val log = LoggerFactory.getLogger(Application::class.java)
    }
}
