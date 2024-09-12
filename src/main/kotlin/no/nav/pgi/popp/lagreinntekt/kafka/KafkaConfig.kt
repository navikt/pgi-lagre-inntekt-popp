package no.nav.pgi.popp.lagreinntekt.kafka

import no.nav.pgi.popp.lagreinntekt.util.getVal
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer

internal const val GROUP_ID = "pgi-lagre-inntekt-consumer-group"
internal const val PGI_INNTEKT_TOPIC = "pensjonopptjening.privat-pgi-inntekt"
internal const val PGI_HENDELSE_REPUBLISERING_TOPIC = "pensjonopptjening.privat-pgi-hendelse-republisering"

internal class KafkaConfig(environment: Map<String, String> = System.getenv(), private val securityStrategy: SecurityStrategy = SslStrategy()) {
    private val bootstrapServers = environment.getVal(BOOTSTRAP_SERVERS)

    internal fun inntektConsumerConfig() = mapOf(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.GROUP_ID_CONFIG to GROUP_ID,
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
    )

    internal fun hendelseProducerConfig() = mapOf(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.ACKS_CONFIG to "all",
        ProducerConfig.RETRIES_CONFIG to Integer.MAX_VALUE
    )

    internal fun commonConfig() = mapOf(
        BOOTSTRAP_SERVERS_CONFIG to bootstrapServers
    ) + securityStrategy.securityConfig()

    internal companion object EnvironmentKeys {
        const val BOOTSTRAP_SERVERS = "KAFKA_BROKERS"
    }

    internal interface SecurityStrategy {
        fun securityConfig(): Map<String, String>
    }
}