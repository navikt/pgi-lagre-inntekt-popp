package no.nav.pgi.popp.lagreinntekt.kafka

import no.nav.pensjon.samhandling.env.getVal
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs

class SaslSslStrategy(environment: Map<String, String> = System.getenv()) : KafkaConfig.SecurityStrategy {

    private val saslJaasConfig = createPlainLoginModule(
            environment.getVal(KAFKA_USERNAME),
            environment.getVal(KAFKA_PASSWORD)
    )

    override fun securityConfig() = mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
            SaslConfigs.SASL_MECHANISM to "PLAIN",
            SaslConfigs.SASL_JAAS_CONFIG to saslJaasConfig
    )

    private fun createPlainLoginModule(username: String, password: String) =
            """org.apache.kafka.common.security.plain.PlainLoginModule required username="$username" password="$password";"""

    private companion object EnvironmentKeys {
        const val KAFKA_USERNAME = "KAFKA_USERNAME"
        const val KAFKA_PASSWORD = "KAFKA_PASSWORD"
    }
}