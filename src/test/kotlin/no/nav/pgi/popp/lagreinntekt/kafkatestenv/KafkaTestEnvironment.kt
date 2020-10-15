package no.nav.pgi.popp.lagreinntekt.kafkatestenv


import no.nav.common.KafkaEnvironment
import no.nav.pgi.popp.lagreinntekt.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.PGI_HENDELSE_TOPIC
import no.nav.pgi.popp.lagreinntekt.PGI_INNTEKT_TOPIC
import org.apache.kafka.clients.CommonClientConfigs

class KafkaTestEnvironment {
    private val kafkaTestEnvironment: KafkaEnvironment = KafkaEnvironment(
            withSchemaRegistry = true,
            topicNames = listOf(PGI_INNTEKT_TOPIC, PGI_HENDELSE_TOPIC)
    )

    init { kafkaTestEnvironment.start() }

    private val schemaRegistryUrl: String
        get() = kafkaTestEnvironment.schemaRegistry!!.url

    internal fun testEnvironment() = mapOf(
            KafkaConfig.BOOTSTRAP_SERVERS to kafkaTestEnvironment.brokersURL,
            KafkaConfig.SCHEMA_REGISTRY_URL to schemaRegistryUrl
    )

    internal fun commonTestConfig() = mapOf(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafkaTestEnvironment.brokersURL,
            "schema.registry.url" to schemaRegistryUrl
    )

    internal fun tearDown() = kafkaTestEnvironment.tearDown()
}
