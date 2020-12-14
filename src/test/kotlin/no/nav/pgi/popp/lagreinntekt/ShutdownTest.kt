package no.nav.pgi.popp.lagreinntekt

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import no.nav.pensjon.samhandling.liveness.IS_ALIVE_PATH
import no.nav.pgi.popp.lagreinntekt.mock.KafkaMockFactory
import no.nav.pgi.popp.lagreinntekt.mock.POPP_MOCK_URL
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer
import org.apache.kafka.common.KafkaException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ShutdownTest {

    private val poppMockServer = PoppMockServer()
    private var kafkaMockFactory = KafkaMockFactory()
    private var application = Application(kafkaMockFactory, mapOf("POPP_URL" to POPP_MOCK_URL))

    @AfterEach
    fun afterEach() {
        kafkaMockFactory.close()
        kafkaMockFactory = KafkaMockFactory()
        application.tearDown()
        application = Application(kafkaMockFactory, mapOf("POPP_URL" to POPP_MOCK_URL))
        poppMockServer.reset()
    }

    @AfterAll
    fun tearDown() {
        poppMockServer.stop()
    }

    @Test
    fun `should close application when exception is thrown`() {
        assertEquals(200, callIsAlive().statusCode())
        kafkaMockFactory.pensjonsgivendeInntektConsumer.setPollException(KafkaException("Test Exception"))

        application.startLagreInntektPopp(false)
        validateClosed()
    }

    @Test
    fun `should close application when stop is called`() {
        assertEquals(200, callIsAlive().statusCode())
        GlobalScope.async {
            delay(50)
            application.stop()
        }

        application.startLagreInntektPopp(true)
        validateClosed()
    }

    private fun validateClosed() {
        assertTrue(kafkaMockFactory.hendelseProducer.closed(), "hendelseProducer.closed")
        assertTrue(kafkaMockFactory.pensjonsgivendeInntektConsumer.closed(), "pensjonsgivendeInntektConsumer.closed")
        assertThrows<Exception> { callIsAlive() }

    }

    private fun callIsAlive() =
            HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080$IS_ALIVE_PATH")).GET().build(), HttpResponse.BodyHandlers.ofString())
}

