package no.nav.pgi.popp.lagreinntekt

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
import no.nav.pgi.popp.lagreinntekt.mock.KafkaMockFactory
import no.nav.pgi.popp.lagreinntekt.mock.POPP_MOCK_URL
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer
import no.nav.pgi.popp.lagreinntekt.mock.TokenProviderMock
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import no.nav.samordning.pgi.schema.PensjonsgivendeInntektPerOrdning
import no.nav.samordning.pgi.schema.Skatteordning.FASTLAND
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LagreInntektPoppTest {

    private val poppMockServer = PoppMockServer()
    private var kafkaMockFactory = KafkaMockFactory()
    private val poppClient = PoppClient(testEnvironment(), TokenProviderMock())
    private var lagreInntektPopp = LagreInntektPopp(poppClient, kafkaMockFactory)

    @AfterEach
    fun afterEach() {
        kafkaMockFactory.close()
        kafkaMockFactory = KafkaMockFactory()
        lagreInntektPopp.stop()
        lagreInntektPopp = LagreInntektPopp(poppClient, kafkaMockFactory)
        poppMockServer.reset()
    }

    @AfterAll
    fun tearDown() {
        kafkaMockFactory.close()
        poppMockServer.stop()
        lagreInntektPopp.stop()
    }

    @Test
    fun `Commits to consumer when stored in POPP`() {
        poppMockServer.`Mock default response 201 ok`()
        val pgiRecords: List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>> = createPgiRecords(5, 15)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        lagreInntektPopp.start(loopForever = false)

        assertEquals(pgiRecords.last().offset() + 1, kafkaMockFactory.committedOffset())
    }

    @Test
    fun `Commits to consumer when republished`() {
        poppMockServer.`Mock default response 500 servererror`()
        val pgiRecords = createPgiRecords(10, 20)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        lagreInntektPopp.start(loopForever = false)

        assertEquals(11, kafkaMockFactory.hendelseProducer.history().size)
        assertEquals(pgiRecords.last().offset() + 1, kafkaMockFactory.committedOffset())
    }

    @Test
    fun `should exits loop on stop`() {
        GlobalScope.async {
            delay(50)
            lagreInntektPopp.stop()
        }
        lagreInntektPopp.start(loopForever = true)

        assertFalse(kafkaMockFactory.hendelseProducer.closed())
        assertFalse(kafkaMockFactory.pensjonsgivendeInntektConsumer.closed())
    }

    @Test
    fun `should close kafka producer and consumer on closeKafka`() {
        GlobalScope.async {
            delay(20)
            lagreInntektPopp.stop()
            delay(20)
            lagreInntektPopp.closeKafka()
        }

        lagreInntektPopp.start(loopForever = true)
        Thread.sleep(100)

        assertTrue(kafkaMockFactory.hendelseProducer.closed())
        assertTrue(kafkaMockFactory.pensjonsgivendeInntektConsumer.closed())
    }

    private fun createPgiRecords(fromOffset: Long, toOffset: Long) = (fromOffset..toOffset)
            .map {
                val pgi = createPgi((it + 10000000000).toString())
                ConsumerRecord(PGI_INNTEKT_TOPIC, KafkaMockFactory.DEFAULT_PARTITION, it, pgi.key(), pgi)
            }

    private fun createPgi(identifikator: String): PensjonsgivendeInntekt =
            PensjonsgivendeInntekt(identifikator,
                    2020L,
                    listOf(PensjonsgivendeInntektPerOrdning(FASTLAND, "2020-01-01", 523000L, 320000L, 2000L, 200L)))

    private fun testEnvironment() = mapOf(
        "POPP_URL" to POPP_MOCK_URL,
        "AZURE_APP_CLIENT_ID" to "1234",
        "AZURE_APP_CLIENT_SECRET" to "verySecret",
        "AZURE_APP_TARGET_API_ID" to "5678",
        "AZURE_APP_WELL_KNOWN_URL" to "https://login.microsoft/asfasf",
    )
}

private fun PensjonsgivendeInntekt.key() = HendelseKey(getNorskPersonidentifikator(), getInntektsaar().toString())
