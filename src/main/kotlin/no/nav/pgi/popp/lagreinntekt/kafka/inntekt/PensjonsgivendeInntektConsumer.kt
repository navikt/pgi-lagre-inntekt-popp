package no.nav.pgi.popp.lagreinntekt.kafka.inntekt

import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

private val TIMEOUT_DURATION = Duration.ofSeconds(4)

internal class PensjonsgivendeInntektConsumer(kafkaFactory: KafkaFactory) {
    private val consumer: Consumer<String, String> = kafkaFactory.pensjonsgivendeInntektConsumer()
    private var closed: AtomicBoolean = AtomicBoolean(false)

    internal fun pollInntektRecords() =
        consumer.poll(TIMEOUT_DURATION).records(PGI_INNTEKT_TOPIC).toList()
            .also { records -> logNumberOfRecordsPolledFromTopic(records) }

    internal fun commit() {
        consumer.commitSync()
    }

    internal fun close() {
        log.info("Closing ${PensjonsgivendeInntektConsumer::class.simpleName}")
        consumer.close()
        closed.set(true)
    }

    internal fun isClosed() = closed.get()

    private fun logNumberOfRecordsPolledFromTopic(consumerRecords: List<ConsumerRecord<String, String>>) {
        if (consumerRecords.isNotEmpty()) log.info("Number of records polled from topic $PGI_INNTEKT_TOPIC: ${consumerRecords.size}")
    }

    private companion object {
        private val log = LoggerFactory.getLogger(PensjonsgivendeInntektConsumer::class.java)
    }
}