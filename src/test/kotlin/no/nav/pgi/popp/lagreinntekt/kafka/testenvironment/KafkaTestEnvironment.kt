package no.nav.pgi.popp.lagreinntekt.kafka.testenvironment


import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_REPUBLISERING_TOPIC
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG

/*
class KafkaTestEnvironment {

    private val kafkaTestEnvironment: KafkaEnvironment = KafkaEnvironment(
            withSchemaRegistry = false,
            topicNames = listOf(PGI_INNTEKT_TOPIC, PGI_HENDELSE_REPUBLISERING_TOPIC)
    )

    init {
        kafkaTestEnvironment.start()
    }

    internal fun testEnvironment() = mapOf(
            KafkaConfig.BOOTSTRAP_SERVERS to kafkaTestEnvironment.brokersURL,
    )

    internal fun commonTestConfig() = mapOf(
            BOOTSTRAP_SERVERS_CONFIG to kafkaTestEnvironment.brokersURL,
    )

    internal fun tearDown() = kafkaTestEnvironment.tearDown()
}
*/