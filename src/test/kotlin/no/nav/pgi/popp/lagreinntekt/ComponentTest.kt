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

private const val INNTEKTSAAR = 2018L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LagreInntektPoppTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testEnvironment(), PlaintextStrategy())
    private val inntektTestProducer: InntektTestProducer = InntektTestProducer(kafkaTestEnvironment.commonTestConfig())
    private val republishedHendelse = HendelseTestConsumer(kafkaTestEnvironment.commonTestConfig())
    private val poppMockServer = PoppMockServer()
    private val lagreInntektPopp = LagreInntektPopp(kafkaConfig, mapOf("POPP_URL" to POPP_MOCK_URL) + kafkaTestEnvironment.testEnvironment())

    @AfterAll
    fun tearDown() {
        kafkaTestEnvironment.tearDown()
        poppMockServer.stop()
        inntektTestProducer.close()
        republishedHendelse.close()
    }

    @Test
    fun `application sends inntekter to popp or republishes them to hendelse topic`() {
        val inntekter = pensjonsgivendeInntekter()
        val invalidInntekter = invalidPensjonsgivendeInntekter()

        populateInntektTopic(inntekter + invalidInntekter)

        lagreInntektPopp.start(loopForever = false)

        assertEquals(invalidInntekter.size, republishedHendelse.getRecords().size)
    }

    private fun populateInntektTopic(inntekter: List<PensjonsgivendeInntekt>) {
        for (inntekt in inntekter) {
            val hendelseKey = HendelseKey(inntekt.getNorskPersonidentifikator(), inntekt.getInntektsaar().toString())
            inntektTestProducer.produceToInntektTopic(hendelseKey, inntekt)
        }
    }

    private fun pensjonsgivendeInntekter() = listOf(
            createPensjonsgivendeInntekt(FNR_NR1_201, INNTEKTSAAR),
            createPensjonsgivendeInntekt(FNR_NR2_201, INNTEKTSAAR),
            createPensjonsgivendeInntekt(FNR_NR3_201, INNTEKTSAAR)
    )

    private fun invalidPensjonsgivendeInntekter() = listOf(
            createPensjonsgivendeInntekt(FNR_NR1_500, INNTEKTSAAR),
            createPensjonsgivendeInntekt(FNR_NR2_500, INNTEKTSAAR),
            createPensjonsgivendeInntekt(FNR_NR3_500, INNTEKTSAAR),
            createPensjonsgivendeInntekt(FNR_NR4_500, INNTEKTSAAR),
            createPensjonsgivendeInntekt(FNR_NR5_500, INNTEKTSAAR),
    )


    private fun createPensjonsgivendeInntekt(norskPersonidentifikator: String, inntektsaar: Long): PensjonsgivendeInntekt {
        val pensjonsgivendeIntekter = mutableListOf<PensjonsgivendeInntektPerOrdning>()
        val pensjonsgivendeInntektPerOrdning = PensjonsgivendeInntektPerOrdning(Skatteordning.FASTLAND, "$inntektsaar-01-01", 523000L, 320000L, 2000L, null)
        pensjonsgivendeIntekter.add(pensjonsgivendeInntektPerOrdning)
        return PensjonsgivendeInntekt(norskPersonidentifikator, inntektsaar, pensjonsgivendeIntekter)
    }
}
