package no.nav.pgi.popp.lagreinntekt.kafka

import no.nav.pgi.popp.lagreinntekt.kafka.testenvironment.InntektTestProducer
import no.nav.pgi.popp.lagreinntekt.kafka.testenvironment.KafkaTestEnvironment
import no.nav.pgi.popp.lagreinntekt.kafka.testenvironment.PlaintextStrategy
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PensjonsgivendeInntektConsumerTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testEnvironment(), PlaintextStrategy())
    private val inntektTestProducer = InntektTestProducer(kafkaTestEnvironment.commonTestConfig())
    private val inntektConsumer = PensjonsgivendeInntektConsumer(kafkaConfig)

    @AfterAll
    fun tearDown() {
        kafkaTestEnvironment.tearDown()
        inntektTestProducer.close()
    }

    @Test
    fun `consume from inntekt topic`() {
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("1234", "2018", emptyList())
        val hendelseKey = HendelseKey("1234", "2018")
        inntektTestProducer.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)

        val inntektRecord = inntektConsumer.getInntekter()
        assertEquals(hendelseKey, inntektRecord[0].key())
        assertEquals(pensjonsgivendeInntekt, inntektRecord[0].value())
    }


}