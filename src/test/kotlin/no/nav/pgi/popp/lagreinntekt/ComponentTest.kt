package no.nav.pgi.popp.lagreinntekt

import no.nav.pgi.popp.lagreinntekt.kafkatestenv.HendelseTestConsumer
import no.nav.pgi.popp.lagreinntekt.kafkatestenv.InntektTestProducer
import no.nav.pgi.popp.lagreinntekt.kafkatestenv.KafkaTestEnvironment
import no.nav.pgi.popp.lagreinntekt.kafkatestenv.PlaintextStrategy
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ComponentTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testEnvironment(), PlaintextStrategy())
    private val inntektTestProducer = InntektTestProducer(kafkaTestEnvironment.commonTestConfig())
    private val hendelseTestConsumer = HendelseTestConsumer(kafkaTestEnvironment.commonTestConfig())
    private val inntektConsumer = PensjonsgivendeInntektConsumer(kafkaConfig)
    private val hendelseProducer = HendelseProducer(kafkaConfig)

    @AfterAll
    fun tearDown() {
        kafkaTestEnvironment.tearDown()
    }

    @Test
    fun `consume from inntekt topic`() {
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("1234", "2018")
        val hendelseKey = HendelseKey("1234", "2018")
        inntektTestProducer.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)

        val inntektRecord = inntektConsumer.getInntekter()

        assertEquals(hendelseKey, inntektRecord[0].key())
        assertEquals(pensjonsgivendeInntekt, inntektRecord[0].value())
    }

    @Test
    fun `when call to lagreInntekt fails then republish to hendelse topic`() {
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("1234", "2018")
        val hendelseKey = HendelseKey("1234", "2018")

        inntektTestProducer.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)
        val inntektRecord = inntektConsumer.getInntekter()

        assertEquals(hendelseKey, inntektRecord[0].key())
        assertEquals(pensjonsgivendeInntekt, inntektRecord[0].value())

        //val poppClient = PoppClient()
        //val response = poppClient.lagreInntekt(inntektRecord[0].value())
        //assertEquals("not 200 ok", response.statusCode)

        hendelseProducer.rePublishHendelse(hendelseKey)

        assertEquals(hendelseKey, hendelseTestConsumer.getFirstHendelseRecord().key())
    }
}
