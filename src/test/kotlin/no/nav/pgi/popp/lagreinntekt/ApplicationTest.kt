package no.nav.pgi.popp.lagreinntekt

import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApplicationTest {
    private val application = createApplication()
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testConfiguration())
    private val inntektConsumer = PensjonsgivendeInntektConsumer(kafkaConfig)

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
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("1234", "2018")
        val hendelseKey = HendelseKey("1234", "2018")
        kafkaTestEnvironment.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)

        val inntekter = inntektConsumer.getInntekter()

        assertTrue(inntekter.isNotEmpty())
        assertEquals(1, inntekter.size)
    }
}