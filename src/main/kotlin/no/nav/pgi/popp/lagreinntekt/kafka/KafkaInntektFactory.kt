package no.nav.pgi.popp.lagreinntekt.kafka

import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer

internal class KafkaInntektFactory(private val kafkaConfig: KafkaConfig = KafkaConfig()) : KafkaFactory {
    override fun pensjonsgivendeInntektConsumer() =
            KafkaConsumer<HendelseKey, PensjonsgivendeInntekt>(kafkaConfig.commonConfig() + kafkaConfig.inntektConsumerConfig())
                    .also { it.subscribe(listOf(PGI_INNTEKT_TOPIC)) }

    override fun hendelseProducer() = KafkaProducer<HendelseKey, Hendelse>(
            kafkaConfig.commonConfig() + kafkaConfig.hendelseProducerConfig())
}

internal interface KafkaFactory {
    fun pensjonsgivendeInntektConsumer(): Consumer<HendelseKey, PensjonsgivendeInntekt>
    fun hendelseProducer(): Producer<HendelseKey, Hendelse>
}