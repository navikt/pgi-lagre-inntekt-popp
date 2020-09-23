package no.nav.pgi.popp.lagreinntekt


import no.nav.common.KafkaEnvironment
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringSerializer

const val KAFKA_TEST_USERNAME = "srvTest"
const val KAFKA_TEST_PASSWORD = "opensourcedPassword"

class KafkaTestEnvironment {
    private val kafkaTestEnvironment: KafkaEnvironment = KafkaEnvironment(topicNames = listOf(KafkaConfig.PGI_INNTEKT_TOPIC))
    private val hendelseTestProducer = inntektTestProducer()

    init {
        kafkaTestEnvironment.start()
    }

    internal fun tearDown() = kafkaTestEnvironment.tearDown()

    internal fun testConfiguration() = mapOf<String, String>(
            KafkaConfig.BOOTSTRAP_SERVERS_ENV_KEY to kafkaTestEnvironment.brokersURL,
            KafkaConfig.USERNAME_ENV_KEY to KAFKA_TEST_USERNAME,
            KafkaConfig.PASSWORD_ENV_KEY to KAFKA_TEST_PASSWORD,
            KafkaConfig.SECURITY_PROTOCOL_ENV_KEY to SecurityProtocol.PLAINTEXT.name
    )

    private fun inntektTestProducer() = KafkaProducer<String, String>(
            inntektTestProducerConfig()
    )

    private fun inntektTestProducerConfig() =
            mapOf(
                    CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafkaTestEnvironment.brokersURL,
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                    ProducerConfig.ACKS_CONFIG to "all",
                    ProducerConfig.RETRIES_CONFIG to Integer.MAX_VALUE
            )

    internal fun produceToInntektTopic(inntektKey: String, inntekt: String) {
        val record = ProducerRecord(KafkaConfig.PGI_INNTEKT_TOPIC, inntektKey, inntekt)
        hendelseTestProducer.send(record).get()
        hendelseTestProducer.flush()
    }
}