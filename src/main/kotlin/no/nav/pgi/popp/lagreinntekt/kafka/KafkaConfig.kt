package no.nav.pgi.popp.lagreinntekt.kafka

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.*
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.pensjon.samhandling.env.getVal
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig

internal const val GROUP_ID = "pgi-lagre-inntekt-consumer-group"
internal const val PGI_INNTEKT_TOPIC = "pensjonopptjening.privat-pgi-inntekt"
internal const val PGI_HENDELSE_TOPIC = "pensjonopptjening.privat-pgi-hendelse"

internal class KafkaConfig(environment: Map<String, String> = System.getenv(), private val securityStrategy: SecurityStrategy = SslStrategy()) {
    private val bootstrapServers = environment.getVal(BOOTSTRAP_SERVERS)
    private val schemaRegUsername = environment.getVal(SCHEMA_REGISTRY_USERNAME)
    private val schemaRegPassword = environment.getVal(SCHEMA_REGISTRY_PASSWORD)
    private val schemaRegistryUrl = environment.getVal(SCHEMA_REGISTRY)

    internal fun inntektConsumerConfig() = mapOf(
        KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
        ConsumerConfig.GROUP_ID_CONFIG to GROUP_ID,
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
    )

    internal fun hendelseProducerConfig() = mapOf(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
        ProducerConfig.ACKS_CONFIG to "all",
        ProducerConfig.RETRIES_CONFIG to Integer.MAX_VALUE
    )

    internal fun commonConfig() = mapOf(
        BOOTSTRAP_SERVERS_CONFIG to bootstrapServers
    ) + securityStrategy.securityConfig() + schemaRegistryConfig()

    private fun schemaRegistryConfig() = mapOf(
        BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
        USER_INFO_CONFIG to "$schemaRegUsername:$schemaRegPassword",
        SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl
    )

    internal companion object EnvironmentKeys {
        const val BOOTSTRAP_SERVERS = "KAFKA_BROKERS"
        const val SCHEMA_REGISTRY = "KAFKA_SCHEMA_REGISTRY"
        const val SCHEMA_REGISTRY_USERNAME = "KAFKA_SCHEMA_REGISTRY_USER"
        const val SCHEMA_REGISTRY_PASSWORD = "KAFKA_SCHEMA_REGISTRY_PASSWORD"
    }

    internal interface SecurityStrategy {
        fun securityConfig(): Map<String, String>
    }
}