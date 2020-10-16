package no.nav.pgi.popp.lagreinntekt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
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

private const val POPP_PORT = 1080
private const val POPP_PATH = "/pgi/lagreinntekt"
private const val POPP_URL = "http://localhost:$POPP_PORT$POPP_PATH"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ComponentTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaConfig = KafkaConfig(kafkaTestEnvironment.testEnvironment(), PlaintextStrategy())
    private val inntektTestProducer = InntektTestProducer(kafkaTestEnvironment.commonTestConfig())
    private val hendelseTestConsumer = HendelseTestConsumer(kafkaTestEnvironment.commonTestConfig())
    private val inntektConsumer = PensjonsgivendeInntektConsumer(kafkaConfig)
    private val hendelseProducer = HendelseProducer(kafkaConfig)
    private val poppApiServer = WireMockServer(POPP_PORT)

    init {
        poppApiServer.start()
        mockHttpResponse500()
    }

    @AfterAll
    fun tearDown() {
        kafkaTestEnvironment.tearDown()
        poppApiServer.stop()
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
        val inntektRecordList = inntektConsumer.getInntekter()

        assertEquals(hendelseKey, inntektRecordList[0].key())
        assertEquals(pensjonsgivendeInntekt, inntektRecordList[0].value())

        val poppClient = PoppClient(POPP_URL)
        val response = poppClient.lagreInntekt(inntektRecordList[0].value())
        assertEquals("500", response.statusCode)

        hendelseProducer.rePublishHendelse(hendelseKey)

        assertEquals(hendelseKey, hendelseTestConsumer.getFirstHendelseRecord().key())
    }

    private fun mockHttpResponse500() {
        poppApiServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo(POPP_PATH))
                        .willReturn(WireMock.okJson(pgiPoppHttp500())))
    }

    private fun pgiPoppHttp500() = "{\n" +
            "  \"personidentifikator\": \"01234567890\",\n" +
            "  \"inntektsaar\": \"2017\",\n" +
            "  \"pensjonsgivendeInntekt\": 100000,\n" +
            "  \"pensjonsgivendeInntektFastland\": 50000,\n" +
            "  \"pensjonsgivendeInntektSvalbard\": 50000\n" +
            "}"

}
