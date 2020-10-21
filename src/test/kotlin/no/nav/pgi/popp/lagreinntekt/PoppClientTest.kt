package no.nav.pgi.popp.lagreinntekt


import no.nav.pgi.popp.lagreinntekt.kafka.*
import no.nav.pgi.popp.lagreinntekt.kafka.HendelseTestConsumer
import no.nav.pgi.popp.lagreinntekt.kafka.InntektTestProducer
import no.nav.pgi.popp.lagreinntekt.kafka.PlaintextStrategy
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
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
    private val hendelseProducer = HendelseProducer(kafkaConfig)
    private val inntektTestProducer = InntektTestProducer(kafkaTestEnvironment.commonTestConfig())
    private val inntektConsumer = PensjonsgivendeInntektConsumer(kafkaConfig)
    private val hendelseTestConsumer = HendelseTestConsumer(kafkaTestEnvironment.commonTestConfig())

    @AfterAll
    fun tearDown() {
        kafkaTestEnvironment.tearDown()
        poppMockServer.stop()
    }

    @Test
    fun `assert POST response, Http status code 201 Created`() {
        val poppClient = PoppClient(POPP_URL)
        val hendelseKey = HendelseKey("1234", "2018")
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("1234", "2018")
        val consumerRecord = ConsumerRecord("topic", 0, 1, hendelseKey, pensjonsgivendeInntekt)

        val response = poppClient.storePensjonsgivendeInntekter(consumerRecord)

        assertEquals(201, response.statusCode())
    }

    @Test
    fun `application gets inntekter, fails to send them to popp, therefore republishes the inntekt hendelse`() {
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("2345", "2018")
        val hendelseKey = HendelseKey("2345", "2018")
        inntektTestProducer.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)
        val inntektRecordList = inntektConsumer.getInntekter()

        assertEquals(hendelseKey, inntektRecordList[0].key())
        assertEquals(pensjonsgivendeInntekt, inntektRecordList[0].value())

        val poppClient = PoppClient(POPP_URL)
        val response = poppClient.storePensjonsgivendeInntekter(inntektRecordList[0])
        assertEquals(500, response.statusCode())

        hendelseProducer.rePublishHendelse(hendelseKey)

        assertEquals(hendelseKey, hendelseTestConsumer.getFirstHendelseRecord()?.key())
    }

}