package no.nav.pgi.popp.lagreinntekt

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.pgi.domain.*
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaInntektFactory
import no.nav.pgi.popp.lagreinntekt.kafka.testenvironment.HendelseTestConsumer
import no.nav.pgi.popp.lagreinntekt.kafka.testenvironment.InntektTestProducer
import no.nav.pgi.popp.lagreinntekt.kafka.testenvironment.KafkaTestEnvironment
import no.nav.pgi.popp.lagreinntekt.kafka.testenvironment.PlaintextStrategy
import no.nav.pgi.popp.lagreinntekt.mock.POPP_MOCK_URL
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR1_200
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR1_409
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR2_200
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR2_409
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR3_200
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR3_409
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR4_409
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer.Companion.FNR_NR5_409
import no.nav.pgi.popp.lagreinntekt.mock.TokenProviderMock
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ComponentTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaFactory = KafkaInntektFactory(
        KafkaConfig(
            kafkaTestEnvironment.testEnvironment(),
            PlaintextStrategy()
        )
    )
    private val inntektTestProducer: InntektTestProducer = InntektTestProducer(kafkaTestEnvironment.commonTestConfig())
    private val republishedHendelse = HendelseTestConsumer(
        kafkaTestEnvironment.commonTestConfig()
    )
    private val poppMockServer = PoppMockServer()
    private val poppClient = PoppClient(
        environment = mapOf(pair = "POPP_URL" to POPP_MOCK_URL),
        tokenProvider = TokenProviderMock()
    )
    private val lagreInntektPopp = LagreInntektPopp(
        poppResponseCounter = PoppResponseCounter(SimpleMeterRegistry()),
        poppClient = poppClient,
        kafkaFactory = kafkaFactory
    )

    @AfterAll
    fun tearDown() {
        kafkaTestEnvironment.tearDown()
        poppMockServer.stop()
        inntektTestProducer.close()
        republishedHendelse.close()
    }

    @Test
    fun `application sends inntekter to popp or republishes them to hendelse topic if status 200 or 409`() {
        val inntekter = pensjonsgivendeInntekterWith200FromPopp()
        val invalidInntekter = pensjonsgivendeInntekterWith409FromPopp()

        populateInntektTopic(inntekter + invalidInntekter)

        lagreInntektPopp.processInntektRecords()
        assertThat(republishedHendelse.getRecords()).hasSameSizeAs(invalidInntekter)
    }

    private fun populateInntektTopic(inntekter: List<PensjonsgivendeInntekt>) {
        inntekter.forEach {
            val hendelseKey = HendelseKey(it.norskPersonidentifikator, it.inntektsaar.toString())
            inntektTestProducer.produceToInntektTopic(hendelseKey, it)
        }
    }

    private fun pensjonsgivendeInntekterWith200FromPopp() = listOf(
        createPensjonsgivendeInntekt(FNR_NR1_200, 2018, PensjonsgivendeInntektMetadata(0, 0)),
        createPensjonsgivendeInntekt(FNR_NR2_200, 2019, PensjonsgivendeInntektMetadata(0, 0)),
        createPensjonsgivendeInntekt(FNR_NR3_200, 2020, PensjonsgivendeInntektMetadata(0, 0))
    )

    private fun pensjonsgivendeInntekterWith409FromPopp() = listOf(
        createPensjonsgivendeInntekt(FNR_NR1_409, 2018, PensjonsgivendeInntektMetadata(1, 2)),
        createPensjonsgivendeInntekt(FNR_NR2_409, 2018, PensjonsgivendeInntektMetadata(3, 4)),
        createPensjonsgivendeInntekt(FNR_NR3_409, 2019, PensjonsgivendeInntektMetadata(5, 6)),
        createPensjonsgivendeInntekt(FNR_NR4_409, 2019, PensjonsgivendeInntektMetadata(7, 8)),
        createPensjonsgivendeInntekt(FNR_NR5_409, 2020, PensjonsgivendeInntektMetadata(9, 10))
    )

    private fun createPensjonsgivendeInntekt(
        idenfifikator: String,
        inntektsaar: Long,
        metadata: PensjonsgivendeInntektMetadata
    ): PensjonsgivendeInntekt {
        val pensjonsgivendeIntekter = mutableListOf<PensjonsgivendeInntektPerOrdning>().apply {
            add(
                PensjonsgivendeInntektPerOrdning(
                    Skatteordning.FASTLAND,
                    "$inntektsaar-01-01",
                    523000L,
                    320000L,
                    2000L,
                    null
                )
            )
        }
        return PensjonsgivendeInntekt(idenfifikator, inntektsaar, pensjonsgivendeIntekter, metadata)
    }
}
