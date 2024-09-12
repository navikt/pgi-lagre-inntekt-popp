package no.nav.pgi.popp.lagreinntekt

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaInntektFactory
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

fun serviceMain() {
    val application = ApplicationService(
        // TODO: Midlertidig
        poppResponseCounter = PoppResponseCounter(Counters(SimpleMeterRegistry()))
    )
    try {
        application.startLagreInntektPopp()
    } catch (e: Exception) {
        application.stop()
    } finally {
        exitProcess(0)
    }
}

internal class ApplicationService(
    poppResponseCounter: PoppResponseCounter,
    kafkaFactory: KafkaFactory = KafkaInntektFactory(),
    env: Map<String, String> = System.getenv(),
) {
    private val poppClient = PoppClient(env)
    private val lagreInntektPopp = LagreInntektPopp(
        poppResponseCounter = poppResponseCounter,
        poppClient,
        kafkaFactory
    )
    private var started = AtomicBoolean(false)


    init {
        addShutdownHook()
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(ApplicationService::class.java)
        private val SECURE_LOG = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun startLagreInntektPopp(loopForever: Boolean = true) {
        try {
            started.set(true)
            lagreInntektPopp.processInntektLoop(loopForever)
        } catch (e: Throwable) {
            LOG.error(e.message)
        } finally {
            tearDown()
        }
    }

    internal fun tearDown() {
        LOG.info("Closing lagreInntektPopp")
        lagreInntektPopp.closeKafka()
    }

    internal fun stop() {
        LOG.info("Stopping naisServer and lagreInntektPopp")
        lagreInntektPopp.stop()
        Thread.sleep(3000)
        if (!lagreInntektPopp.isClosed()) {
            LOG.info("Failed to stop lagreInntektPopp")
            tearDown()
        }
    }

    private fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                LOG.info("Stopping application from shutdown hook")
                stop()
            } catch (e: Exception) {
                LOG.info("Shutdownhook failed trying to close kafka")
                tearDown()
            }
        })
    }
}