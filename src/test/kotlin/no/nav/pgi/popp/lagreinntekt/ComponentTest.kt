package no.nav.pgi.popp.lagreinntekt

import no.nav.pgi.popp.lagreinntekt.kafka.*
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val POPP_PORT = 1080
private const val POPP_PATH = "/pgi/lagreinntekt"
private const val POPP_URL = "http://localhost:$POPP_PORT$POPP_PATH"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ComponentTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testEnvironment(), PlaintextStrategy())
    private val inntektTestProducer: InntektTestProducer = InntektTestProducer(kafkaTestEnvironment.commonTestConfig())
    private val hendelseTestConsumer = HendelseTestConsumer(kafkaTestEnvironment.commonTestConfig())
    private val poppMockServer = PoppMockServer()
    private val application = Application(kafkaConfig)

    @AfterAll
    fun tearDown() {
        kafkaTestEnvironment.tearDown()
        poppMockServer.stop()
        inntektTestProducer.close()
        hendelseTestConsumer.close()
    }

    @Test
    fun `application gets inntekter and sends them to popp`() {
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("1234", "2018")
        val hendelseKey = HendelseKey("1234", "2018")
        inntektTestProducer.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)

        application.storePensjonsgivendeInntekterInPopp(mapOf("POPP_URL" to POPP_URL), loopForever = false)

        assertEquals(null, hendelseTestConsumer.getFirstHendelseRecord())

    }

    @Test
    fun `republish when POPP returns 500 Internal Server Error`() {
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("2345", "2018")
        val hendelseKey = HendelseKey("2345", "2018")
        inntektTestProducer.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)

        application.storePensjonsgivendeInntekterInPopp(mapOf("POPP_URL" to POPP_URL), loopForever = false)

        val republishedHendelse = hendelseTestConsumer.getFirstHendelseRecord()
        assertNotNull(republishedHendelse)
        assertEquals(hendelseKey, republishedHendelse?.key())
        assertEquals(pensjonsgivendeInntekt.getIdentifikator(), republishedHendelse?.value()?.getIdentifikator())

    }

}
