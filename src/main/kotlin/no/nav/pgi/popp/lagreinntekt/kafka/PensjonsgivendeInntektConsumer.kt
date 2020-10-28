package no.nav.pgi.popp.lagreinntekt.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

private val TIMEOUT_DURATION = Duration.ofSeconds(4)

internal class PensjonsgivendeInntektConsumer(kafkaConfig: KafkaConfig) {
    private val consumer = KafkaConsumer<HendelseKey, PensjonsgivendeInntekt>(kafkaConfig.commonConfig() + inntektConsumerConfig())

    init {
        consumer.subscribe(listOf(PGI_INNTEKT_TOPIC))
    }

    internal fun getInntekter() = consumer.poll(TIMEOUT_DURATION).records(PGI_INNTEKT_TOPIC).toList()

    internal fun commit() {
        consumer.commitSync()
    }
    private fun inntektConsumerConfig() = mapOf(
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to GROUP_ID,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest" //TODO: Needs checking
    )
}