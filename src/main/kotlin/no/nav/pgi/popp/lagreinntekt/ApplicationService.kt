package no.nav.pgi.popp.lagreinntekt

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaInntektFactory
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

fun serviceMain() {
    val applicationStatus = ApplicationStatus().setStarted()
    val application = ApplicationService(
        // TODO: Midlertidig
        poppResponseCounter = PoppResponseCounter(Counters(SimpleMeterRegistry())),
        applicationStatus = applicationStatus,
    )
    try {
        do {
            application.startLagreInntektPopp()
        } while (!applicationStatus.isStopped())
        application.tearDown()
    } catch (e: Exception) {
        application.stop()
    } finally {
        exitProcess(0)
    }
}

internal class ApplicationService(
    poppResponseCounter: PoppResponseCounter,
    private val applicationStatus: ApplicationStatus,
    kafkaFactory: KafkaFactory = KafkaInntektFactory(),
    env: Map<String, String> = System.getenv(),
) {
    private var stop: AtomicBoolean = AtomicBoolean(false)

    private val poppClient = PoppClient(env)
    private val lagreInntektPopp = LagreInntektPopp(
        poppResponseCounter = poppResponseCounter,
        poppClient,
        kafkaFactory
    )


    init {
        addShutdownHook()
    }

    internal fun startLagreInntektPopp(loopForever: Boolean = true) {
        try {
            lagreInntektPopp.processInntektRecords()
        } catch (e: Throwable) {
            LOG.error(e.message)
            applicationStatus.setStopped()
        }
    }

    internal fun tearDown() {
        LOG.info("Closing lagreInntektPopp")
        lagreInntektPopp.closeKafka()
    }

    internal fun stop() {
        LOG.info("Stopping lagreInntektPopp")
        stop.set(true)
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

    private companion object {
        private val LOG = LoggerFactory.getLogger(ApplicationService::class.java)
        private val SECURE_LOG = LoggerFactory.getLogger("tjenestekall")
    }
}