package no.nav.pgi.popp.lagreinntekt


import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaTestEnvironment
import no.nav.pgi.popp.lagreinntekt.kafka.PlaintextStrategy
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


private const val POPP_PORT = 1080
private const val POPP_PATH = "/pgi/lagreinntekt"
private const val POPP_URL = "http://localhost:$POPP_PORT$POPP_PATH"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PoppClientTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val poppMockServer = PoppMockServer()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testEnvironment(), PlaintextStrategy())

    @AfterAll
    fun tearDown() {
        kafkaTestEnvironment.tearDown()
        poppMockServer.stop()
    }

    @Test
    fun `assert no rePublish to Hendelse`() {
        val poppClient = PoppClient(POPP_URL)
        val hendelseKey = HendelseKey("1000", "2018")
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("1000", "2018")
        val consumerRecord = ConsumerRecord("topic", 0, 1, hendelseKey, pensjonsgivendeInntekt)
        val inntekter = listOf(consumerRecord)
        val rePublishToHendelse = poppClient.savePensjonsgivendeInntekter(inntekter)

        assertTrue(rePublishToHendelse.isEmpty())
    }
}