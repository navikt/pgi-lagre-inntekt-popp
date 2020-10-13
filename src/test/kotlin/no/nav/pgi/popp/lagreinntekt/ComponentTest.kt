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
internal class ComponentTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testConfiguration(), PlaintextStrategy())
    private val inntektConsumer = PensjonsgivendeInntektConsumer(kafkaConfig)

    @AfterAll
    fun tearDown() {
        kafkaTestEnvironment.tearDown()
    }

    @Test
    fun `consume from inntekt topic`() {
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("1234", "2018")
        val hendelseKey = HendelseKey("1234", "2018")
        kafkaTestEnvironment.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)

        val inntektRecord = inntektConsumer.getInntekter()

        assertEquals(hendelseKey, inntektRecord[0].key())
        assertEquals(pensjonsgivendeInntekt, inntektRecord[0].value())
    }
}
