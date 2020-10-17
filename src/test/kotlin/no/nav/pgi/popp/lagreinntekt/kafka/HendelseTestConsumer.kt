package no.nav.pgi.popp.lagreinntekt.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

internal class HendelseTestConsumer(commonKafkaConfig: Map<String, String>) {

    private val hendelseTestConsumer = KafkaConsumer<HendelseKey, Hendelse>(commonKafkaConfig + hendelseTestConsumerConfig())

    init {
        hendelseTestConsumer.subscribe(listOf(PGI_HENDELSE_TOPIC))
    }

    private fun hendelseTestConsumerConfig() = mapOf(
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to "test-id",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
    )

    fun getFirstHendelseRecord() : ConsumerRecord<HendelseKey, Hendelse> {
        val hendelseRecords = hendelseTestConsumer.poll(Duration.ofSeconds(4)).records(PGI_HENDELSE_TOPIC).toList()
        return hendelseRecords[0]
    }
}