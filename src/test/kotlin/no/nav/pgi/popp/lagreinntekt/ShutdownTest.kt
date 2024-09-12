package no.nav.pgi.popp.lagreinntekt

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.pgi.popp.lagreinntekt.mock.KafkaMockFactory
import no.nav.pgi.popp.lagreinntekt.mock.POPP_MOCK_URL
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer
import org.apache.kafka.common.KafkaException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestInstance

// TODO: Må skrives helt om får å gi mening
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ShutdownTest {

    private val poppMockServer = PoppMockServer()
    private var kafkaMockFactory = KafkaMockFactory()
    private var application = ApplicationService(
        poppResponseCounter = PoppResponseCounter(Counters(SimpleMeterRegistry())),
        kafkaFactory = kafkaMockFactory,
        env = testEnvironment(),
        applicationStatus = ApplicationStatus(),
        exitApplication = {},
    )

    @AfterEach
    fun afterEach() {
        kafkaMockFactory.close()
        kafkaMockFactory = KafkaMockFactory()
        application.terminate()
        application = ApplicationService(
            poppResponseCounter = PoppResponseCounter(Counters(SimpleMeterRegistry())),
            kafkaFactory = kafkaMockFactory,
            env = testEnvironment(),
            applicationStatus = ApplicationStatus(),
            exitApplication = {},
        )
        poppMockServer.reset()
    }

    @AfterAll
    fun tearDown() {
        poppMockServer.stop()
    }

    // TODO: Felles ApplicationService for flere tester, burde opprette per instans    @Test
    fun `should close application when exception is thrown`() {
        kafkaMockFactory.pensjonsgivendeInntektConsumer.setPollException(KafkaException("Test Exception"))
        application.processInntektRecordsIteration()

        validateClosed()
    }

    private fun validateClosed() {
        assertTrue(kafkaMockFactory.hendelseProducer.closed(), "hendelseProducer.closed")
        assertTrue(kafkaMockFactory.pensjonsgivendeInntektConsumer.closed(), "pensjonsgivendeInntektConsumer.closed")
    }

    private fun testEnvironment() = mapOf(
        "POPP_URL" to POPP_MOCK_URL,
        "AZURE_APP_CLIENT_ID" to "1234",
        "AZURE_APP_CLIENT_SECRET" to "verySecret",
        "AZURE_APP_TARGET_API_ID" to "5678",
        "AZURE_APP_WELL_KNOWN_URL" to "https://login.microsoft/asfasf",
    )
}

