package no.nav.pgi.popp.lagreinntekt.kafka.inntekt

import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.time.Duration

private val TIMEOUT_DURATION = Duration.ofSeconds(4)

internal class PensjonsgivendeInntektConsumer(kafkaFactory: KafkaFactory) {
    private val consumer: Consumer<HendelseKey, PensjonsgivendeInntekt> = kafkaFactory.pensjonsgivendeInntektConsumer()

    init {
        consumer.subscribe(listOf(PGI_INNTEKT_TOPIC))
    }

    internal fun pollInntektRecords() =
            consumer.poll(TIMEOUT_DURATION).records(PGI_INNTEKT_TOPIC).toList()
                    .also { records -> logNumberOfRecordsPolledFromTopic(records) }

    internal fun commit() {
        consumer.commitSync()
    }

    private fun logNumberOfRecordsPolledFromTopic(consumerRecords: List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>) {
        LOG.info("Number of records polled from topic $PGI_INNTEKT_TOPIC: ${consumerRecords.size}")
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(PensjonsgivendeInntektConsumer::class.java)
    }
}