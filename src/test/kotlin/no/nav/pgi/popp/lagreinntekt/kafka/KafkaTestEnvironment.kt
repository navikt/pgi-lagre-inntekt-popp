package no.nav.pgi.popp.lagreinntekt.kafka


import no.nav.common.KafkaEnvironment
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
            "KAFKA_SCHEMA_REGISTRY_TEST" to schemaRegistryUrl
    )

    internal fun commonTestConfig() = mapOf(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafkaTestEnvironment.brokersURL,
            "schema.registry.url" to schemaRegistryUrl
    )

    internal fun tearDown() = kafkaTestEnvironment.tearDown()
}
