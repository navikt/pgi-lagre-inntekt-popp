package no.nav.pgi.popp.lagreinntekt

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.pgi.domain.*
import no.nav.pgi.domain.serialization.PgiDomainSerializer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_REPUBLISERING_TOPIC
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
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
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils

@SpringBootTest(classes = [KafkaAutoConfiguration::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EmbeddedKafka(
    partitions = 1,
    topics = [
        PGI_INNTEKT_TOPIC,
        PGI_HENDELSE_REPUBLISERING_TOPIC,
    ],
)
internal class ComponentTest {

    @Autowired
    lateinit var embeddedKafka: EmbeddedKafkaBroker

    lateinit var pgiInntektProducer: Producer<String, String>
    lateinit var pgiInntektConsumer: Consumer<String, String>
    lateinit var republiserHendelseProducer: Producer<String, String>
    lateinit var republiserHendelseConsumer: Consumer<String, String>
    lateinit var kafkaFactory: KafkaFactory

    @BeforeEach
    fun setup() {
        println("%%%% PARTITIONS PER TOPIC: ${embeddedKafka.partitionsPerTopic}")
        println("%%%% TOPICS ${embeddedKafka.topics}")

        val producerProps = KafkaTestUtils.producerProps(embeddedKafka)
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        val producerFactory: ProducerFactory<String, String> = DefaultKafkaProducerFactory(producerProps)

        republiserHendelseProducer = producerFactory.createProducer()
        pgiInntektProducer = producerFactory.createProducer()

        val consumerProps = KafkaTestUtils.consumerProps("no.nav.pgi", "true", embeddedKafka).apply {
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        }
        val consumerFactory = DefaultKafkaConsumerFactory(consumerProps, StringDeserializer(), StringDeserializer())
        pgiInntektConsumer = consumerFactory.createConsumer()
        republiserHendelseConsumer = consumerFactory.createConsumer()

        embeddedKafka.consumeFromAnEmbeddedTopic(pgiInntektConsumer, PGI_INNTEKT_TOPIC)
//        embeddedKafka.consumeFromAnEmbeddedTopic(republiserHendelseConsumer, PGI_HENDELSE_REPUBLISERING_TOPIC)

        kafkaFactory = KafkaTestFactory(pgiInntektConsumer, republiserHendelseProducer)
    }


    @Test
    fun fooTest() {
        println(embeddedKafka)
        println(pgiInntektConsumer)
        println(republiserHendelseProducer)
    }

    private val poppMockServer = PoppMockServer()

    private val poppClient = PoppClient(
        environment = mapOf(pair = "POPP_URL" to POPP_MOCK_URL),
        tokenProvider = TokenProviderMock()
    )

    private fun lagreInntektPopp() = LagreInntektPopp(
        poppResponseCounter = PoppResponseCounter(SimpleMeterRegistry()),
        poppClient = poppClient,
        kafkaFactory = kafkaFactory
    )

    @AfterAll
    fun tearDown() {
        poppMockServer.stop()
    }

    @Test
    fun `application sends inntekter to popp or republishes them to hendelse topic if status 200 or 409`() {
        val inntekter = pensjonsgivendeInntekterWith200FromPopp()
        val invalidInntekter = pensjonsgivendeInntekterWith409FromPopp()

        populateInntektTopic(inntekter + invalidInntekter)

        lagreInntektPopp().processInntektRecords()
        val records = KafkaTestUtils.getRecords(republiserHendelseConsumer)
        assertThat(records).hasSameSizeAs(invalidInntekter)
    }

    @Test
    fun `test test`() {
        lagreInntektPopp().processInntektRecords()
        republiserHendelseProducer.send(ProducerRecord("hello", "world"))
        val records = KafkaTestUtils.getRecords(republiserHendelseConsumer)
        assertThat(records).isEqualTo(42)
    }


    private fun populateInntektTopic(inntekter: List<PensjonsgivendeInntekt>) {
        inntekter.forEach {
            val hendelseKey = HendelseKey(it.norskPersonidentifikator, it.inntektsaar.toString())
            produceToInntektTopic(hendelseKey, it)
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

    class KafkaTestFactory(
        private val consumer: Consumer<String, String>,
        private val producer: Producer<String, String>
    ) : KafkaFactory {

        override fun hendelseProducer(): Producer<String, String> {
            return producer
        }

        override fun pensjonsgivendeInntektConsumer(): Consumer<String, String> {
            return consumer
        }
    }

    internal fun produceToInntektTopic(hendelseKey: HendelseKey, pensjonsgivendeInntekt: PensjonsgivendeInntekt) {
        val key = PgiDomainSerializer().toJson(hendelseKey)
        val value = PgiDomainSerializer().toJson(pensjonsgivendeInntekt)
        val record = ProducerRecord(PGI_INNTEKT_TOPIC, key, value)
        pgiInntektProducer.send(record).get()
    }

}
