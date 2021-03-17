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
import no.nav.samordning.pgi.schema.PensjonsgivendeInntektMetadata
import no.nav.samordning.pgi.schema.PensjonsgivendeInntektPerOrdning
import no.nav.samordning.pgi.schema.Skatteordning.FASTLAND
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LagreInntektPoppTest {

    private val poppMockServer = PoppMockServer()
    private var kafkaMockFactory = KafkaMockFactory()
    private val poppClient = PoppClient(testEnvironment(), TokenProviderMock())
    private var lagreInntektPopp = LagreInntektPopp(poppClient, kafkaMockFactory)

    companion object {
        private const val RETRIES = 1L
        private const val SEKVENSNUMMER = 2L
    }

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
    fun `Commits when 200 is returned from POPP`() {
        poppMockServer.`Mock 200 ok`()
        val pgiRecords: List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>> = createPgiRecords(5, 15)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        lagreInntektPopp.start(loopForever = false)

        assertEquals(pgiRecords.last().offset() + 1, kafkaMockFactory.committedOffset())
    }

    @Test
    fun `Republishes hendelse to consumer when 409 is returned from popp`() {
        poppMockServer.`Mock 409 conflict`()
        val pgiRecords = createPgiRecords(10, 20)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        lagreInntektPopp.start(loopForever = false)

        val republishedHendelser = kafkaMockFactory.hendelseProducer.history()
        assertEquals(11, republishedHendelser.size)
        assertEquals(pgiRecords.last().offset() + 1, kafkaMockFactory.committedOffset())
    }

    @Test
    fun `Republishes hendelse should map retries and sekvensnummer from metadata`() {
        poppMockServer.`Mock 409 conflict`()
        val pgiRecord = createPgiRecords(1, 1).first()

        kafkaMockFactory.addRecord(pgiRecord)
        lagreInntektPopp.start(loopForever = false)

        val republishedHendelseValue = kafkaMockFactory.hendelseProducer.history().first().value()

        assertEquals(pgiRecord.value().getMetaData().getRetries() + 1, republishedHendelseValue.getMetaData().getRetries())
        assertEquals(pgiRecord.value().getMetaData().getSekvensnummer(), republishedHendelseValue.getSekvensnummer())
    }

    @Test
    fun `Republishes hendelse should map fnr, periode and key from key`() {
        poppMockServer.`Mock 409 conflict`()
        val pgiRecord = createPgiRecords(1, 1).first()

        kafkaMockFactory.addRecord(pgiRecord)
        lagreInntektPopp.start(loopForever = false)

        val republishedHendelseKey = kafkaMockFactory.hendelseProducer.history().first().key()

        assertEquals(pgiRecord.value().getInntektsaar(), republishedHendelseKey.getGjelderPeriode().toLong())
        assertEquals(pgiRecord.value().getNorskPersonidentifikator(), republishedHendelseKey.getIdentifikator())
        assertEquals(pgiRecord.key(), republishedHendelseKey)
    }


    @Test
    fun `Throws error when 500 is returned from popp`() {
        poppMockServer.`Mock 500 server error`()
        val pgiRecords = createPgiRecords(10, 20)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        assertThrows<UnhandledStatusCodePoppException> { lagreInntektPopp.start(loopForever = false) }
    }

    @Test
    fun `should exit loop on stop`() {
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
        PensjonsgivendeInntekt(
            identifikator,
            2020L,
            listOf(PensjonsgivendeInntektPerOrdning(FASTLAND, "2020-01-01", 523000L, 320000L, 2000L, 200L)),
            PensjonsgivendeInntektMetadata(RETRIES, SEKVENSNUMMER)
        )

    private fun testEnvironment() = mapOf(
        "POPP_URL" to POPP_MOCK_URL,
        "AZURE_APP_CLIENT_ID" to "1234",
        "AZURE_APP_CLIENT_SECRET" to "verySecret",
        "AZURE_APP_TARGET_API_ID" to "5678",
        "AZURE_APP_WELL_KNOWN_URL" to "https://login.microsoft/asfasf",
    )
}

private fun PensjonsgivendeInntekt.key() = HendelseKey(getNorskPersonidentifikator(), getInntektsaar().toString())
