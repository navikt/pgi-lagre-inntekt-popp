package no.nav.pgi.popp.lagreinntekt

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val SERVER_PORT = 8080
private const val HOST = "http://localhost:$SERVER_PORT"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApplicationTest {
    private val application = createApplication()
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testConfiguration())
    private val inntektConsumer = InntektConsumer(kafkaConfig)

    @BeforeAll
    fun init() {
        application.start()
    }

    @AfterAll
    fun tearDown() {
        application.stop(100, 100)
        kafkaTestEnvironment.tearDown()
    }

    @Test
    fun `validate consuming from kafka topic`() {
        kafkaTestEnvironment.produceToInntektTopic("morradi", "10000")
        val inntekter = inntektConsumer.getInntekter()

        assertTrue(inntekter.isNotEmpty())
        assertEquals("10000", inntekter[0].value())
        assertEquals(1, inntekter.size)
    }


}