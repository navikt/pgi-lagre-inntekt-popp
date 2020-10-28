package no.nav.pgi.popp.lagreinntekt

import no.nav.pgi.popp.lagreinntekt.kafka.*
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
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
    private val pgiLagreInntektPopp = PgiLagreInntektPopp(kafkaConfig, mapOf("POPP_URL" to POPP_URL))

    @AfterAll
    fun tearDown() {
        kafkaTestEnvironment.tearDown()
        poppMockServer.stop()
        inntektTestProducer.close()
        hendelseTestConsumer.close()
    }

    @Test
    fun `application gets inntekter and sends them to popp`() {
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("1000", "2018")
        val hendelseKey = HendelseKey("1000", "2018")
        inntektTestProducer.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)

        pgiLagreInntektPopp.start(loopForever = false)
        Thread.sleep(1000)
        assertEquals(null, hendelseTestConsumer.getFirstHendelseRecord())
    }

    @Test
    fun `republish when POPP returns 500 Internal Server Error`() {
        val pensjonsgivendeInntektID2222Popp500 = PensjonsgivendeInntekt("2222", "2018")
        val hendelseKeyID2222Popp500 = HendelseKey("2222", "2018")
        inntektTestProducer.produceToInntektTopic(hendelseKeyID2222Popp500, pensjonsgivendeInntektID2222Popp500)

        pgiLagreInntektPopp.start(loopForever = false)
        Thread.sleep(1000)

        val republishedHendelse = hendelseTestConsumer.getFirstHendelseRecord()
        assertNotNull(republishedHendelse)
        assertEquals(hendelseKeyID2222Popp500, republishedHendelse?.key())
        assertEquals(pensjonsgivendeInntektID2222Popp500.getIdentifikator(), republishedHendelse?.value()?.getIdentifikator())

    }

    @Test
    fun `republiser siste hendelse naar POPP-respons 500`() {
        val pensjonsgivendeInntektID3000Popp201 = PensjonsgivendeInntekt("3000", "2018")
        val hendelseKeyPoppID3000201 = HendelseKey("3000", "2018")
        val pensjonsgivendeInntektID3333Popp500 = PensjonsgivendeInntekt("3333", "2018")
        val hendelseKeyID3333Popp500 = HendelseKey("3333", "2018")
        inntektTestProducer.produceToInntektTopic(hendelseKeyPoppID3000201, pensjonsgivendeInntektID3000Popp201)
        inntektTestProducer.produceToInntektTopic(hendelseKeyID3333Popp500, pensjonsgivendeInntektID3333Popp500)

        pgiLagreInntektPopp.start(loopForever = false)
        Thread.sleep(1000)

        val republishedHendelse = hendelseTestConsumer.getFirstHendelseRecord()
        assertEquals(pensjonsgivendeInntektID3333Popp500.getIdentifikator(), republishedHendelse?.value()?.getIdentifikator())

    }

    @Test
    fun `republiser foerste hendelse naar POPP-respons er 500`() {
        val pensjonsgivendeInntektID4000Popp201 = PensjonsgivendeInntekt("4000", "2018")
        val hendelseKeyID4000Popp201 = HendelseKey("4000", "2018")
        val pensjonsgivendeInntektID4444Popp500 = PensjonsgivendeInntekt("4444", "2018")
        val hendelseKeyID4444Popp500 = HendelseKey("4444", "2018")
        inntektTestProducer.produceToInntektTopic(hendelseKeyID4444Popp500, pensjonsgivendeInntektID4444Popp500)
        inntektTestProducer.produceToInntektTopic(hendelseKeyID4000Popp201, pensjonsgivendeInntektID4000Popp201)

        pgiLagreInntektPopp.start(loopForever = false)
        Thread.sleep(1000)

        val republishedHendelse = hendelseTestConsumer.getFirstHendelseRecord()
        assertEquals(pensjonsgivendeInntektID4444Popp500.getIdentifikator(), republishedHendelse?.value()?.getIdentifikator())

    }

    @Test
    fun `republiser hendelser naar POPP-respons er 500`() {
        val pensjonsgivendeInntektID5555PoppHttpResponse500 = PensjonsgivendeInntekt("5555", "2018")
        val hendelseKeyID5555Popp500 = HendelseKey("5555", "2018")
        val pensjonsgivendeInntektPoppID5556HttpResponse500 = PensjonsgivendeInntekt("5556", "2018")
        val hendelseKeyID5556Popp500 = HendelseKey("5556", "2018")
        inntektTestProducer.produceToInntektTopic(hendelseKeyID5555Popp500, pensjonsgivendeInntektID5555PoppHttpResponse500)
        inntektTestProducer.produceToInntektTopic(hendelseKeyID5556Popp500, pensjonsgivendeInntektPoppID5556HttpResponse500)
        Thread.sleep(1000)

        pgiLagreInntektPopp.start(loopForever = false)

        val republishedHendelser = hendelseTestConsumer.getRecords()
                .map { consumerRecord -> consumerRecord.value() }

        assertEquals(2, republishedHendelser.size)
        assertTrue(republishedHendelser.contains(Hendelse(-1, "5555", "2018")))
        assertTrue(republishedHendelser.contains(Hendelse(-1, "5556", "2018")))

    }
}
