package no.nav.pgi.popp.lagreinntekt

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaInntektFactory
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

fun serviceMain() {
    val applicationStatus = ApplicationStatus()
    val application = ApplicationService(
        // TODO: Midlertidig
        poppResponseCounter = PoppResponseCounter(Counters(SimpleMeterRegistry())),
        applicationStatus = applicationStatus,
        exitApplication = {
            exitProcess(0)
        }
    )
    try {
        do {
            application.runIteration()
        } while (!applicationStatus.isStopped())
    } catch (e: Exception) {
        applicationStatus.setStopped()
    } finally {
        exitProcess(0)
    }
}

class ApplicationService(
    poppResponseCounter: PoppResponseCounter,
    private val applicationStatus: ApplicationStatus,
    kafkaFactory: KafkaFactory = KafkaInntektFactory(),
    env: Map<String, String> = System.getenv(),
    private val exitApplication: () -> Unit,
) {
    private var stop: AtomicBoolean = AtomicBoolean(false)

    private val poppClient = PoppClient(env)
    private val lagreInntektPopp = LagreInntektPopp(
        poppResponseCounter = poppResponseCounter,
        poppClient = poppClient,
        kafkaFactory = kafkaFactory
    )

    // TODO: 0.2 sekunder, bare sånn at det ikke dreper kibana hvis noe går helt galt
    @Scheduled(fixedDelay = 200L)
    fun runIteration() {
        if (applicationStatus.isActive()) {
            LOG.info("runIteration: processing records")
            processInntektRecordsIteration()
        } else {
            terminate()
        }
    }

    fun terminate() {
        LOG.info("Application is stopping: Closing kafka topics")
        try {
            lagreInntektPopp.closeKafka()
        } catch (e: Exception) {
            if (!lagreInntektPopp.isClosed()) {
                LOG.info("Failed to stop lagreInntektPopp, trying again")
                Thread.sleep(3000)
                lagreInntektPopp.closeKafka()
            }
        } finally {
            exitApplication()
        }
    }

    internal fun processInntektRecordsIteration() {
        try {
            lagreInntektPopp.processInntektRecords()
        } catch (e: Throwable) {
            LOG.error(e.message)
            applicationStatus.setStopped()
        }
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(ApplicationService::class.java)
        private val SECURE_LOG = LoggerFactory.getLogger("tjenestekall")
    }
}