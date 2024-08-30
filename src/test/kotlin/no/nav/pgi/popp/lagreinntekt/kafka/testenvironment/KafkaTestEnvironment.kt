package no.nav.pgi.popp.lagreinntekt.kafka.testenvironment


import no.nav.common.KafkaEnvironment
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_REPUBLISERING_TOPIC
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG

class KafkaTestEnvironment {
    private val kafkaTestEnvironment: KafkaEnvironment = KafkaEnvironment(
            withSchemaRegistry = true,
            topicNames = listOf(PGI_INNTEKT_TOPIC, PGI_HENDELSE_REPUBLISERING_TOPIC)
    )

    init {
        kafkaTestEnvironment.start()
    }

    private val schemaRegistryUrl: String
        get() = kafkaTestEnvironment.schemaRegistry!!.url

    internal fun testEnvironment() = mapOf(
            KafkaConfig.BOOTSTRAP_SERVERS to kafkaTestEnvironment.brokersURL,
            KafkaConfig.SCHEMA_REGISTRY to schemaRegistryUrl,
            KafkaConfig.SCHEMA_REGISTRY_USERNAME to "mrOpenSource",
            KafkaConfig.SCHEMA_REGISTRY_PASSWORD to "opensourcedPassword"
    )

    internal fun commonTestConfig() = mapOf(
            BOOTSTRAP_SERVERS_CONFIG to kafkaTestEnvironment.brokersURL,
    )

    internal fun tearDown() = kafkaTestEnvironment.tearDown()
}
