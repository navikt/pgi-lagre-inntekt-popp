package no.nav.pgi.popp.lagreinntekt.kafka

import no.nav.pensjon.samhandling.env.getVal
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG

internal const val GROUP_ID = "pgi-lagre-inntekt-consumer-group"
internal const val PGI_INNTEKT_TOPIC = "pensjonsamhandling.privat-pgi-inntekt"
internal const val PGI_HENDELSE_TOPIC = "pensjonsamhandling.privat-pgi-hendelse"

internal class KafkaConfig(environment: Map<String, String> = System.getenv(), private val securityStrategy: SecurityStrategy = SslStrategy()) {
    private val bootstrapServers = environment.getVal(BOOTSTRAP_SERVERS)
    private val schemaRegistryUrl = "bogus.url"

    internal fun commonConfig() = mapOf(
            BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            "schema.registry.url" to schemaRegistryUrl
    ) + securityStrategy.securityConfig()

    internal companion object EnvironmentKeys {
        const val BOOTSTRAP_SERVERS = "KAFKA_BROKERS"
        const val SCHEMA_REGISTRY_URL = "KAFKA_SCHEMA_REGISTRY"
    }

    internal interface SecurityStrategy {
        fun securityConfig(): Map<String, String>
    }
}