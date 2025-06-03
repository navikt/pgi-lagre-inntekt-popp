package no.nav.pgi.popp.lagreinntekt

import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaInntektFactory
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import no.nav.pgi.popp.lagreinntekt.util.maskFnr
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled

class ApplicationService(
    poppResponseCounter: PoppResponseCounter,
    private val applicationStatus: ApplicationStatus,
    kafkaFactory: KafkaFactory = KafkaInntektFactory(),
    env: Map<String, String> = System.getenv(),
    private val exitApplication: () -> Unit,
) {
    private val poppClient = PoppClient(env)
    private val lagreInntektPopp = LagreInntektPopp(
        poppResponseCounter = poppResponseCounter,
        poppClient = poppClient,
        kafkaFactory = kafkaFactory
    )

    // TODO: 0.2 sekunder, sånn at det ikke dreper kibana hvis noe går galt
    @Scheduled(fixedDelay = 200L)
    fun runIteration() {
        if (applicationStatus.isActive()) {
            processInntektRecordsIteration()
        } else {
            log.info("runIteration: terminating")
            terminate()
        }
    }

    fun terminate() {
        log.info("Application is stopping: Closing kafka topics")
        try {
            lagreInntektPopp.closeKafka()
        } catch (e: Exception) {
            if (!lagreInntektPopp.isClosed()) {
                log.info("Failed to stop lagreInntektPopp, trying again")
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
            log.error(e.message?.maskFnr())
            secureLog.error(e.message, e)
            applicationStatus.setStopped()
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(ApplicationService::class.java)
        private val secureLog = LoggerFactory.getLogger("team")
    }
}