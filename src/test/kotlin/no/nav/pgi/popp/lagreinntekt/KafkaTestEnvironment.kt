package no.nav.pgi.popp.lagreinntekt


import no.nav.common.KafkaEnvironment
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.security.auth.SecurityProtocol
import io.confluent.kafka.serializers.KafkaAvroSerializer

const val KAFKA_TEST_USERNAME = "srvTest"
const val KAFKA_TEST_PASSWORD = "opensourcedPassword"

class KafkaTestEnvironment {
    private val kafkaTestEnvironment: KafkaEnvironment = KafkaEnvironment(
            withSchemaRegistry = true,
            topicNames = listOf(PGI_INNTEKT_TOPIC, PGI_HENDELSE_TOPIC)
    )
    private val hendelseTestProducer = inntektTestProducer()

    init {
        kafkaTestEnvironment.start()
    }

    private val schemaRegistryUrl: String
        get() = kafkaTestEnvironment.schemaRegistry!!.url

    internal fun tearDown() = kafkaTestEnvironment.tearDown()

    internal fun testConfiguration() = mapOf(
            KafkaConfig.EnvironmentKeys.BOOTSTRAP_SERVERS to kafkaTestEnvironment.brokersURL,
            KafkaConfig.EnvironmentKeys.SCHEMA_REGISTRY_URL to schemaRegistryUrl,
    )

    private fun inntektTestProducer() = KafkaProducer<HendelseKey, PensjonsgivendeInntekt>(inntektTestProducerConfig())

    private fun inntektTestProducerConfig() = mapOf(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafkaTestEnvironment.brokersURL,
            "schema.registry.url" to schemaRegistryUrl,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to Integer.MAX_VALUE
    )

    internal fun produceToInntektTopic(hendelseKey: HendelseKey, pensjonsgivendeInntekt: PensjonsgivendeInntekt) {
        val record = ProducerRecord(PGI_INNTEKT_TOPIC, hendelseKey, pensjonsgivendeInntekt)
        hendelseTestProducer.send(record)
        hendelseTestProducer.flush()
    }
}