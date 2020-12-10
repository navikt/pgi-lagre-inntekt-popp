package no.nav.pgi.popp.lagreinntekt

import no.nav.pensjon.samhandling.naisserver.naisServer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaInntektFactory
import org.slf4j.LoggerFactory

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
        try {
            lagreInntektPopp.start(loopForever)
        } catch (e: Throwable) {
            LOG.error(e.message)
        } finally {
            close()
        }
    }

    internal fun close() {
        LOG.info("Closing naisServer and lagreInntektPopp")
        lagreInntektPopp.stop()
        naisServer.stop(100, 100)
        Thread.sleep(100)
        lagreInntektPopp.closeKafka()
    }

    private fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                close()
            } catch (e: Exception) {
                LOG.info("Shutdownhook faild trying to close kafka")
                lagreInntektPopp.closeKafka()
            }
        })
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(Application::class.java)
    }
}

