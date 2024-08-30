package no.nav.pgi.popp.lagreinntekt.kafka.testenvironment

import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_REPUBLISERING_TOPIC
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration

internal class HendelseTestConsumer(commonKafkaConfig: Map<String, String>) {

    private val hendelseTestConsumer = KafkaConsumer<String, String>(commonKafkaConfig + hendelseTestConsumerConfig())

    init {
        hendelseTestConsumer.subscribe(listOf(PGI_HENDELSE_REPUBLISERING_TOPIC))
    }

    internal fun close() {
        hendelseTestConsumer.close()
    }

    private fun hendelseTestConsumerConfig() = mapOf(
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to "test-id",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
    )

    internal fun getRecords(): List<ConsumerRecord<String, String>> =
            hendelseTestConsumer.poll(Duration.ofSeconds(4)).records(PGI_HENDELSE_REPUBLISERING_TOPIC).toList()
}