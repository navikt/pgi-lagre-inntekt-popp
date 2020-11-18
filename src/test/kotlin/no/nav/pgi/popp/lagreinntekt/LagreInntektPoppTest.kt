package no.nav.pgi.popp.lagreinntekt

import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.testenvironment.HendelseTestConsumer
import no.nav.pgi.popp.lagreinntekt.kafka.testenvironment.InntektTestProducer
import no.nav.pgi.popp.lagreinntekt.kafka.testenvironment.KafkaTestEnvironment
import no.nav.pgi.popp.lagreinntekt.kafka.testenvironment.PlaintextStrategy
import no.nav.pgi.popp.lagreinntekt.mock.POPP_MOCK_URL
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR1_201
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR1_500
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR2_201
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR2_500
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR3_201
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR3_500
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR4_500
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR5_500
import no.nav.samordning.pgi.schema.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val INNTEKEKTSAAR = "2018"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LagreInntektPoppTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testEnvironment(), PlaintextStrategy())
    private val inntektTestProducer: InntektTestProducer = InntektTestProducer(kafkaTestEnvironment.commonTestConfig())
    private val hendelseTestConsumer = HendelseTestConsumer(kafkaTestEnvironment.commonTestConfig())
    private val poppMockServer = PoppMockServer()
    private val lagreInntektPopp = LagreInntektPopp(kafkaConfig, mapOf("POPP_URL" to POPP_MOCK_URL))

    @AfterAll
    fun tearDown() {
        kafkaTestEnvironment.tearDown()
        poppMockServer.stop()
        inntektTestProducer.close()
        hendelseTestConsumer.close()
    }

    @Test
    fun `application gets inntekter and sends them to popp`() {
        val pensjonsgivendeInntekt = createPensjonsgivendeInntekt(FNR_NR1_201, INNTEKEKTSAAR)
        val hendelseKey = HendelseKey(FNR_NR1_201, INNTEKEKTSAAR)
        inntektTestProducer.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)

        lagreInntektPopp.start(loopForever = false)
        Thread.sleep(1000)
        assertEquals(null, hendelseTestConsumer.getFirstHendelseRecord())
    }

    @Test
    fun `republish when POPP returns 500 Internal Server Error`() {
        val pensjonsgivendeInntekt = createPensjonsgivendeInntekt(FNR_NR1_500, INNTEKEKTSAAR)
        val hendelseKey = HendelseKey(FNR_NR1_500, INNTEKEKTSAAR)
        inntektTestProducer.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)

        lagreInntektPopp.start(loopForever = false)
        Thread.sleep(1000)

        val republishedHendelse = hendelseTestConsumer.getFirstHendelseRecord()
        assertNotNull(republishedHendelse)
        assertEquals(hendelseKey, republishedHendelse?.key())
        assertEquals(pensjonsgivendeInntekt.getNorskPersonidentifikator(), republishedHendelse?.value()?.getIdentifikator())

    }

    @Test
    fun `republish last hendelse when POPP returns status code 500`() {
        val pensjonsgivendeInntekt201 = createPensjonsgivendeInntekt(FNR_NR2_201, INNTEKEKTSAAR)
        val hendelseKeyPopp201 = HendelseKey(FNR_NR2_201, INNTEKEKTSAAR)
        val pensjonsgivendeInntekt500 = createPensjonsgivendeInntekt(FNR_NR2_500, INNTEKEKTSAAR)
        val hendelseKey500 = HendelseKey(FNR_NR2_500, INNTEKEKTSAAR)

        inntektTestProducer.produceToInntektTopic(hendelseKeyPopp201, pensjonsgivendeInntekt201)
        inntektTestProducer.produceToInntektTopic(hendelseKey500, pensjonsgivendeInntekt500)

        lagreInntektPopp.start(loopForever = false)
        Thread.sleep(1000)

        val republishedHendelse = hendelseTestConsumer.getFirstHendelseRecord()
        assertEquals(pensjonsgivendeInntekt500.getNorskPersonidentifikator(), republishedHendelse?.value()?.getIdentifikator())

    }

    @Test
    fun `republish first hendelse when POPP returns status code 500`() {

        val pensjonsgivendeInntekt500 = createPensjonsgivendeInntekt(FNR_NR3_500, INNTEKEKTSAAR)
        val hendelseKey500 = HendelseKey(FNR_NR3_500, INNTEKEKTSAAR)
        val pensjonsgivendeInntekt201 = createPensjonsgivendeInntekt(FNR_NR3_201, INNTEKEKTSAAR)
        val hendelseKey201 = HendelseKey(FNR_NR3_201, INNTEKEKTSAAR)

        inntektTestProducer.produceToInntektTopic(hendelseKey500, pensjonsgivendeInntekt500)
        inntektTestProducer.produceToInntektTopic(hendelseKey201, pensjonsgivendeInntekt201)

        lagreInntektPopp.start(loopForever = false)
        Thread.sleep(1000)

        val republishedHendelse = hendelseTestConsumer.getFirstHendelseRecord()
        assertEquals(pensjonsgivendeInntekt500.getNorskPersonidentifikator(), republishedHendelse?.value()?.getIdentifikator())

    }

    @Test
    fun `republish hendelser when POPP returns status code 500`() {
        val pensjonsgivendeInntekt1 = createPensjonsgivendeInntekt(FNR_NR4_500, INNTEKEKTSAAR)
        val hendelseKey1 = HendelseKey(FNR_NR4_500, INNTEKEKTSAAR)

        val pensjonsgivendeInntekt2 = createPensjonsgivendeInntekt(FNR_NR5_500, INNTEKEKTSAAR)
        val hendelseKey2 = HendelseKey(FNR_NR5_500, INNTEKEKTSAAR)

        inntektTestProducer.produceToInntektTopic(hendelseKey1, pensjonsgivendeInntekt1)
        inntektTestProducer.produceToInntektTopic(hendelseKey2, pensjonsgivendeInntekt2)

        Thread.sleep(1000)

        lagreInntektPopp.start(loopForever = false)
        val republishedHendelser = hendelseTestConsumer.getRecords().map { consumerRecord -> consumerRecord.value() }

        assertEquals(2, republishedHendelser.size)
        assertTrue(republishedHendelser.contains(Hendelse(-1, FNR_NR4_500, INNTEKEKTSAAR)))
        assertTrue(republishedHendelser.contains(Hendelse(-1, FNR_NR5_500, INNTEKEKTSAAR)))
    }

    private fun createPensjonsgivendeInntekt(norskPersonidentifikator: String, inntektsaar: String): PensjonsgivendeInntekt {
        val pensjonsgivendeIntekter = mutableListOf<PensjonsgivendeInntektPerOrdning>()
        val pensjonsgivendeInntektPerOrdning = PensjonsgivendeInntektPerOrdning(Skatteordning.FASTLAND, "$inntektsaar-01-01", 523000L, 320000L, 2000L, null)
        pensjonsgivendeIntekter.add(pensjonsgivendeInntektPerOrdning)
        return PensjonsgivendeInntekt(norskPersonidentifikator, inntektsaar, pensjonsgivendeIntekter)
    }
}
