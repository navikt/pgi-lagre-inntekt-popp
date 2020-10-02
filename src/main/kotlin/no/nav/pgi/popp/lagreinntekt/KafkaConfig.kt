package no.nav.pgi.popp.lagreinntekt

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol.SASL_SSL

internal const val GROUP_ID = "pgi-lagre-inntekt-consumer-group"

internal class KafkaConfig(environment: Map<String, String> = System.getenv()) {
    private val bootstrapServers = environment.getVal(BOOTSTRAP_SERVERS_ENV_KEY)
    private val schemaRegistryUrl = environment.getVal(SCHEMA_REGISTRY_URL_ENV_KEY)
    private val saslMechanism = environment.getVal(SASL_MECHANISM_ENV_KEY, "PLAIN")
    private val securityProtocol = environment.getVal(SECURITY_PROTOCOL_ENV_KEY, SASL_SSL.name)
    private val saslJaasConfig = createSaslJaasConfig(
            environment.getVal(USERNAME_ENV_KEY),
            environment.getVal(PASSWORD_ENV_KEY)
    )

    internal fun inntektConsumer() = KafkaConsumer<HendelseKey, PensjonsgivendeInntekt>(inntektConsumerConfig())

    private fun inntektConsumerConfig() = mapOf(
            BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            "schema.registry.url" to schemaRegistryUrl, //TODO: Find constant for schema.registry.url String
            SECURITY_PROTOCOL_CONFIG to securityProtocol,
            SaslConfigs.SASL_MECHANISM to saslMechanism,
            SaslConfigs.SASL_JAAS_CONFIG to saslJaasConfig,
            KEY_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            GROUP_ID_CONFIG to GROUP_ID,
            ENABLE_AUTO_COMMIT_CONFIG to false,
            AUTO_OFFSET_RESET_CONFIG to "earliest" //TODO: Needs checking
    )

    private fun createSaslJaasConfig(username: String, password: String) =
            """org.apache.kafka.common.security.plain.PlainLoginModule required username="$username" password="$password";"""

    companion object {
        const val BOOTSTRAP_SERVERS_ENV_KEY = "KAFKA_BOOTSTRAP_SERVERS"
        const val SCHEMA_REGISTRY_URL_ENV_KEY = "KAFKA_SCHEMA_REGISTRY"
        const val USERNAME_ENV_KEY = "USERNAME"
        const val PASSWORD_ENV_KEY = "PASSWORD"
        const val SASL_MECHANISM_ENV_KEY = "KAFKA_SASL_MECHANISM"
        const val SECURITY_PROTOCOL_ENV_KEY = "KAFKA_SECURITY_PROTOCOL"
        const val PGI_INNTEKT_TOPIC = "privat-pgi-inntekt"
    }
}