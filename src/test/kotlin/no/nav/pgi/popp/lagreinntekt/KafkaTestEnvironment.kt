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
            topicNames = listOf(KafkaConfig.PGI_INNTEKT_TOPIC)
    )
    private val hendelseTestProducer = inntektTestProducer()

    init {
        kafkaTestEnvironment.start()
    }

    private val schemaRegistryUrl: String
        get() = kafkaTestEnvironment.schemaRegistry!!.url

    internal fun tearDown() = kafkaTestEnvironment.tearDown()

    internal fun testConfiguration() = mapOf(
            KafkaConfig.BOOTSTRAP_SERVERS_ENV_KEY to kafkaTestEnvironment.brokersURL,
            KafkaConfig.SCHEMA_REGISTRY_URL_ENV_KEY to schemaRegistryUrl,
            KafkaConfig.USERNAME_ENV_KEY to KAFKA_TEST_USERNAME,
            KafkaConfig.PASSWORD_ENV_KEY to KAFKA_TEST_PASSWORD,
            KafkaConfig.SECURITY_PROTOCOL_ENV_KEY to SecurityProtocol.PLAINTEXT.name
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
        val record = ProducerRecord(KafkaConfig.PGI_INNTEKT_TOPIC, hendelseKey, pensjonsgivendeInntekt)
        hendelseTestProducer.send(record)
        hendelseTestProducer.flush()
    }
}