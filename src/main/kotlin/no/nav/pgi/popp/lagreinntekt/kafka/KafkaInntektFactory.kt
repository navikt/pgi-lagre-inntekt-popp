package no.nav.pgi.popp.lagreinntekt.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer

internal class KafkaInntektFactory(private val kafkaConfig: KafkaConfig = KafkaConfig()) : KafkaFactory {
    override fun pensjonsgivendeInntektConsumer() =
        KafkaConsumer<String, String>(kafkaConfig.commonConfig() + kafkaConfig.inntektConsumerConfig())
            .also { it.subscribe(listOf(PGI_INNTEKT_TOPIC)) }

    override fun hendelseProducer() = KafkaProducer<String, String>(
        kafkaConfig.commonConfig() + kafkaConfig.hendelseProducerConfig()
    )
}

internal interface KafkaFactory {
    fun pensjonsgivendeInntektConsumer(): Consumer<String, String>
    fun hendelseProducer(): Producer<String, String>
}