package no.nav.pgi.popp.lagreinntekt

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import no.nav.pgi.domain.*
import no.nav.pgi.domain.serialization.PgiDomainSerializer
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
import no.nav.pgi.popp.lagreinntekt.mock.KafkaMockFactory
import no.nav.pgi.popp.lagreinntekt.mock.POPP_MOCK_URL
import no.nav.pgi.popp.lagreinntekt.mock.PoppMockServer
import no.nav.pgi.popp.lagreinntekt.mock.TokenProviderMock
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LagreInntektPoppTest {

    private val poppMockServer = PoppMockServer()
    private var kafkaMockFactory = KafkaMockFactory()
    private val poppClient = PoppClient(
        environment = testEnvironment(),
        tokenProvider = TokenProviderMock()
    )
    private var lagreInntektPopp = LagreInntektPopp(
        poppResponseCounter = PoppResponseCounter(SimpleMeterRegistry()),
        poppClient = poppClient,
        kafkaFactory = kafkaMockFactory
    )

    companion object {
        private const val RETRIES = 1L
        private const val SEKVENSNUMMER = 2L
    }

    @AfterEach
    fun afterEach() {
        kafkaMockFactory.close()
        kafkaMockFactory = KafkaMockFactory()
        lagreInntektPopp = LagreInntektPopp(
            poppResponseCounter = PoppResponseCounter(SimpleMeterRegistry()),
            poppClient = poppClient,
            kafkaFactory = kafkaMockFactory
        )
        poppMockServer.reset()
    }

    @AfterAll
    fun tearDown() {
        kafkaMockFactory.close()
        poppMockServer.stop()
    }

    @Test
    fun `Commits when 200 is returned from POPP`() {
        poppMockServer.`Mock 200 ok`()
        val pgiRecords = createPgiRecords(1, 3)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        lagreInntektPopp.processInntektRecords()

        assertEquals(pgiRecords.last().offset() + 1, kafkaMockFactory.committedOffset())
    }


    @Test
    fun `Commits when 400 with body PGI_001_PID_VALIDATION_FAILED is returned from POPP`() {
        poppMockServer.`Mock 400 bad request with body`("PGI_001_PID_VALIDATION_FAILED bla bla bla")
        val pgiRecords = createPgiRecords(5, 6)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        lagreInntektPopp.processInntektRecords()

        assertEquals(pgiRecords.last().offset() + 1, kafkaMockFactory.committedOffset())
    }

    @Test
    fun `Commits when 400 with body PGI_002_INNTEKT_AAR_VALIDATION_FAILED is returned from POPP`() {
        poppMockServer.`Mock 400 bad request with body`("PGI_002_INNTEKT_AAR_VALIDATION_FAILED")
        val pgiRecords = createPgiRecords(100, 102)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        lagreInntektPopp.processInntektRecords()

        assertEquals(pgiRecords.last().offset() + 1, kafkaMockFactory.committedOffset())
    }

    @Test
    fun `Throws error when 400 with empty body is returned from popp`() {
        poppMockServer.`Mock 400 bad request with body`("")
        val pgiRecords = createPgiRecords(15, 20)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        assertThrows<UnhandledStatusCodePoppException> { lagreInntektPopp.processInntektRecords() }
    }

    @Test
    fun `Throws error when 400 with unknown body is returned from popp`() {
        poppMockServer.`Mock 400 bad request with body`("Unknown body")
        val pgiRecords = createPgiRecords(10, 20)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        assertThrows<UnhandledStatusCodePoppException> { lagreInntektPopp.processInntektRecords() }
    }

    @Test
    fun `Republishes hendelse to consumer when 409 Bruker ekisterer ikke i PEN is returned from popp`() {
        poppMockServer.`Mock 409 Bruker eksisterer ikke i PEN`()
        val pgiRecords = createPgiRecords(10, 12)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        lagreInntektPopp.processInntektRecords()

        val republishedHendelser = kafkaMockFactory.hendelseProducer.history()
        assertThat(republishedHendelser).hasSize(3)
        assertEquals(pgiRecords.last().offset() + 1, kafkaMockFactory.committedOffset())
    }

    @Test
    fun `Republishes hendelse to consumer when 409 Fant ikke person is returned from popp`() {
        poppMockServer.`Mock 409 Fant ikke person`()
        val pgiRecords = createPgiRecords(13, 16)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        lagreInntektPopp.processInntektRecords()

        val republishedHendelser = kafkaMockFactory.hendelseProducer.history()
        assertThat(republishedHendelser).hasSize(4)
        assertEquals(pgiRecords.last().offset() + 1, kafkaMockFactory.committedOffset())
    }

    @Test
    fun `Republishes hendelse should map retries and sekvensnummer from metadata`() {
        poppMockServer.`Mock 409 Bruker eksisterer ikke i PEN`()
        val pgiRecord = createPgiRecords(1, 1).first()

        kafkaMockFactory.addRecord(pgiRecord)
        lagreInntektPopp.processInntektRecords()

        val republishedHendelseValue =
            PgiDomainSerializer().fromJson(
                Hendelse::class,
                kafkaMockFactory.hendelseProducer.history().first().value()
            )

        val value = PgiDomainSerializer().fromJson(PensjonsgivendeInntekt::class, pgiRecord.value())
        assertEquals(value.metaData.retries, republishedHendelseValue.metaData.retries)
        assertEquals(value.metaData.sekvensnummer, republishedHendelseValue.sekvensnummer)
    }

    @Test
    fun `Republishes hendelse should map fnr, periode and key from key`() {
        poppMockServer.`Mock 409 Bruker eksisterer ikke i PEN`()
        val pgiRecord = createPgiRecords(1, 1).first()

        kafkaMockFactory.addRecord(pgiRecord)
        lagreInntektPopp.processInntektRecords()

        val republishedHendelseKey = PgiDomainSerializer().fromJson(
            HendelseKey::class,
            kafkaMockFactory.hendelseProducer.history().first().key()
        )

        val value = PgiDomainSerializer().fromJson(PensjonsgivendeInntekt::class, pgiRecord.value())
        assertEquals(value.inntektsaar, republishedHendelseKey.gjelderPeriode.toLong())
        assertEquals(value.norskPersonidentifikator, republishedHendelseKey.identifikator)
        assertThat(PgiDomainSerializer().fromJson(HendelseKey::class, pgiRecord.key()))
            .isEqualTo(republishedHendelseKey)
    }


    @Test
    fun `Throws error when 500 is returned from popp`() {
        poppMockServer.`Mock 500 server error`()
        val pgiRecords = createPgiRecords(10, 20)

        pgiRecords.forEach { kafkaMockFactory.addRecord(it) }
        assertThrows<UnhandledStatusCodePoppException> {
            lagreInntektPopp.processInntektRecords()
        }
    }

    @Test
    fun `should exit loop on stop`() {
        GlobalScope.async {
            delay(50)
        }
        lagreInntektPopp.processInntektRecords()

        assertFalse(kafkaMockFactory.hendelseProducer.closed())
        assertFalse(kafkaMockFactory.pensjonsgivendeInntektConsumer.closed())
    }

    @Test
    fun `should close kafka producer and consumer on closeKafka`() {
        GlobalScope.async {
            delay(20)
            lagreInntektPopp.closeKafka()
        }

        lagreInntektPopp.processInntektRecords()
        Thread.sleep(100)

        assertTrue(kafkaMockFactory.hendelseProducer.closed())
        assertTrue(kafkaMockFactory.pensjonsgivendeInntektConsumer.closed())
    }

    private fun createPgiRecords(fromOffset: Long, toOffset: Long) = (fromOffset..toOffset)
        .map {
            val pgi = createPgi((it + 10000000000).toString())
            val key = PgiDomainSerializer().toJson(pgi.key())
            val value = PgiDomainSerializer().toJson(pgi)
            ConsumerRecord(PGI_INNTEKT_TOPIC, KafkaMockFactory.DEFAULT_PARTITION, it, key, value)
        }

    private fun createPgi(identifikator: String): PensjonsgivendeInntekt =
        PensjonsgivendeInntekt(
            identifikator,
            2020L,
            listOf(
                PensjonsgivendeInntektPerOrdning(
                    Skatteordning.FASTLAND,
                    "2020-01-01",
                    523000L,
                    320000L,
                    2000L,
                    200L
                )
            ),
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

private fun PensjonsgivendeInntekt.key() = HendelseKey(norskPersonidentifikator, inntektsaar.toString())
