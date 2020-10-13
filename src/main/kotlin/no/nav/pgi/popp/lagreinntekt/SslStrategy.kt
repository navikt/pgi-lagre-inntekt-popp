package no.nav.pgi.popp.lagreinntekt

import no.nav.pgi.popp.lagreinntekt.SslStrategy.EnvironmentKeys.SSL_KEYSTORE_LOCATION
import no.nav.pgi.popp.lagreinntekt.SslStrategy.EnvironmentKeys.SSL_KEYSTORE_PASSWORD
import no.nav.pgi.popp.lagreinntekt.SslStrategy.EnvironmentKeys.SSL_TRUSTSTORE_LOCATION
import no.nav.pgi.popp.lagreinntekt.SslStrategy.EnvironmentKeys.SSL_TRUSTSTORE_PASSWORD
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol

private const val JAVA_KEYSTORE = "jks"
private const val PKCS12 = "PKCS12"

internal class SslStrategy(environment: Map<String, String> = System.getenv()) : KafkaConfig.SecurityStrategy {

    private val sslKeystoreLocation = environment.getVal(SSL_KEYSTORE_LOCATION)
    private val sslKeystorePassword = environment.getVal(SSL_KEYSTORE_PASSWORD)
    private val sslTruststoreLocation = environment.getVal(SSL_TRUSTSTORE_LOCATION)
    private val sslTruststorePassword = environment.getVal(SSL_TRUSTSTORE_PASSWORD)

    override fun securityConfig() = mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "", //Disable server host name verification
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to JAVA_KEYSTORE,
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to PKCS12,
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to sslTruststoreLocation,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to sslTruststorePassword,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to sslKeystoreLocation,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to sslKeystorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to sslKeystorePassword
    )

    private object EnvironmentKeys {
        const val SSL_TRUSTSTORE_LOCATION = "KAFKA_TRUSTSTORE_PATH"
        const val SSL_TRUSTSTORE_PASSWORD = "KAFKA_CREDSTORE_PASSWORD"
        const val SSL_KEYSTORE_LOCATION = "KAFKA_KEYSTORE_PATH"
        const val SSL_KEYSTORE_PASSWORD = "KAFKA_CREDSTORE_PASSWORD"
    }
}
