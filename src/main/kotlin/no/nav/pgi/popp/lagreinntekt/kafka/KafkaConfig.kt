package no.nav.pgi.popp.lagreinntekt.kafka

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.*
import no.nav.pensjon.samhandling.env.getVal
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG

internal const val GROUP_ID = "pgi-lagre-inntekt-consumer-group"
internal const val PGI_INNTEKT_TOPIC = "pensjonsamhandling.privat-pgi-inntekt"
internal const val PGI_HENDELSE_TOPIC = "pensjonsamhandling.privat-pgi-hendelse"

internal class KafkaConfig(environment: Map<String, String> = System.getenv(), private val securityStrategy: SecurityStrategy = SslStrategy()) {
    private val bootstrapServers = environment.getVal(BOOTSTRAP_SERVERS)
    private val schemaRegUsername = environment.getVal(SCHEMA_REGISTRY_USERNAME)
    private val schemaRegPassword = environment.getVal(SCHEMA_REGISTRY_PASSWORD)
    private val schemaRegistryUrl = environment.getVal(SCHEMA_REGISTRY)

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