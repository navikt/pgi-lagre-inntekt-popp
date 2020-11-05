package no.nav.pgi.popp.lagreinntekt.popp


import no.nav.pgi.popp.lagreinntekt.kafka.testenvironment.KafkaTestEnvironment
import no.nav.pgi.popp.lagreinntekt.mock.POPP_MOCK_URL
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR1_201
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PoppClientTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val poppMockServer = PoppMockServer()
    private val pgiPopp = PGIPopp(POPP_MOCK_URL)

    @AfterAll
    fun tearDown() {
        kafkaTestEnvironment.tearDown()
        poppMockServer.stop()
    }

    @Test
    fun `assert no rePublish to Hendelse`() {
        val hendelseKey = HendelseKey(FNR_NR1_201, "2018")
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt(FNR_NR1_201, "2018", emptyList())
        val consumerRecord = ConsumerRecord("topic", 0, 1, hendelseKey, pensjonsgivendeInntekt)
        val inntekter = listOf(consumerRecord)
        val rePublishToHendelse = pgiPopp.savePensjonsgivendeInntekter(inntekter)

        assertTrue(rePublishToHendelse.isEmpty())
    }
}