package no.nav.pgi.popp.lagreinntekt.mock

import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration

internal class KafkaMockFactory(
    internal val hendelseProducer: MockProducer<String, String> = defaultHendelseProducer(),
    internal val pensjonsgivendeInntektConsumer: MockConsumer<String, String> = defaultPensjonsgivendeInntektConsumer(),
) : KafkaFactory {
    override fun hendelseProducer(): Producer<String, String> = hendelseProducer
    override fun pensjonsgivendeInntektConsumer(): Consumer<String, String> = pensjonsgivendeInntektConsumer

    internal fun close() {
        hendelseProducer.apply { if (!closed()) close() }
        pensjonsgivendeInntektConsumer.apply { if (!closed()) close() }
    }

    internal fun addRecord(record: ConsumerRecord<String, String>) {
        pensjonsgivendeInntektConsumer.addRecord(record)
    }

    internal fun committedOffset(): Long {
        val topicPartition = TopicPartition(PGI_INNTEKT_TOPIC, 0)
        return pensjonsgivendeInntektConsumer.committed(
            mutableSetOf(topicPartition),
            Duration.ofSeconds(2)
        )[topicPartition]!!.offset()
    }

    companion object {
        internal const val DEFAULT_PARTITION = 0

        internal fun defaultHendelseProducer() = MockProducer<String, String>(true, StringSerializer(), StringSerializer())

        internal fun defaultPensjonsgivendeInntektConsumer() =
            MockConsumer<String, String>(OffsetResetStrategy.EARLIEST)
                .apply {
                    val topicPartition = TopicPartition(PGI_INNTEKT_TOPIC, DEFAULT_PARTITION)
                    assign(listOf(topicPartition))
                    updateBeginningOffsets(mapOf(topicPartition to 0))
                }
    }
}


