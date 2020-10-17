package no.nav.pgi.popp.lagreinntekt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.HendelseTestConsumer
import no.nav.pgi.popp.lagreinntekt.kafka.InntektTestProducer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaTestEnvironment
import no.nav.pgi.popp.lagreinntekt.kafka.PlaintextStrategy
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

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

    @BeforeAll
    fun setUp() {
        poppApiServer.start()
        mockHttpResponse500FromPopp()
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

    @Disabled("Under construction")
    @Test
    fun `application gets inntekter and sends them to popp`() {
        mockHttpResponse200FromPopp()
        val application = Application(kafkaConfig)
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("1234", "2018")
        val hendelseKey = HendelseKey("1234", "2018")
        inntektTestProducer.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)

        application.storePensjonsgivendeInntekterInPopp(mapOf("POPP_URL" to POPP_URL), loopForever = false)

        //assertEquals(null, )
    }


    @Test
    fun `application gets inntekter, fails to send them to popp, therefore republishes the inntekt hendelse`() {
        val pensjonsgivendeInntekt = PensjonsgivendeInntekt("1234", "2018")
        val hendelseKey = HendelseKey("1234", "2018")

        inntektTestProducer.produceToInntektTopic(hendelseKey, pensjonsgivendeInntekt)
        val inntektRecordList = inntektConsumer.getInntekter()

        assertEquals(hendelseKey, inntektRecordList[0].key())
        assertEquals(pensjonsgivendeInntekt, inntektRecordList[0].value())

        val poppClient = PoppClient(POPP_URL)
        val response = poppClient.storePensjonsgivendeInntekter(inntektRecordList[0].value())
        assertEquals(500, response.statusCode)

        hendelseProducer.rePublishHendelse(hendelseKey)

        assertEquals(hendelseKey, hendelseTestConsumer.getFirstHendelseRecord().key())
    }

    private fun mockHttpResponse500FromPopp() {
        poppApiServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo(POPP_PATH))
                        .willReturn(WireMock.serverError()))
    }

    private fun mockHttpResponse200FromPopp() {
        poppApiServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo(POPP_PATH))
                        .willReturn(WireMock.okJson("{}")))
    }
}
