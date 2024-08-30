package no.nav.pgi.popp.lagreinntekt.kafka.republish

import net.logstash.logback.marker.Markers
import no.nav.pgi.domain.Hendelse
import no.nav.pgi.domain.HendelseKey
import no.nav.pgi.domain.HendelseMetadata
import no.nav.pgi.domain.PensjonsgivendeInntekt
import no.nav.pgi.domain.serialization.PgiDomainSerializer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_REPUBLISERING_TOPIC
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

internal class RepubliserHendelseProducer(kafkaFactory: KafkaFactory) {
    private val producer: Producer<String, String> = kafkaFactory.hendelseProducer()
    private var closed: AtomicBoolean = AtomicBoolean(false)

    companion object {
        private val LOG = LoggerFactory.getLogger(RepubliserHendelseProducer::class.java)
        private val SECURE_LOG = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun send(consumerRecord: ConsumerRecord<String, String>) {
        val value = toHendelse(consumerRecord)
        val hendelseRecord =
            ProducerRecord(
                PGI_HENDELSE_REPUBLISERING_TOPIC,
                consumerRecord.key(),
                PgiDomainSerializer().toJson(value)
            )
        producer.send(hendelseRecord).get()
        LOG.info(
            Markers.append("sekvensnummer", value.sekvensnummer),
            "Republiserer hendelse. Sekvensnummer: ${
                value.sekvensnummer
            } Retries: ${value.metaData.retries}"
        )
        SECURE_LOG.info(
            Markers.append("sekvensnummer", value.sekvensnummer),
            "Republiserer hendelse. Sekvensnummer: ${
                value.sekvensnummer
            } Retries: ${
                value.metaData.retries
            } Values: $value to $PGI_HENDELSE_REPUBLISERING_TOPIC"
        )
    }

    internal fun close() {
        LOG.info("Closing ${RepubliserHendelseProducer::class.simpleName}")
        producer.close()
        closed.set(true)
    }

    internal fun isClosed() = closed.get()

    private fun toHendelse(consumerRecord: ConsumerRecord<String, String>): Hendelse {
        val pensjonsgivendeInntekt =
            PgiDomainSerializer().fromJson(PensjonsgivendeInntekt::class, consumerRecord.value())
        val hendelseKey = PgiDomainSerializer().fromJson(HendelseKey::class, consumerRecord.key())
        return Hendelse(
            pensjonsgivendeInntekt.metaData.sekvensnummer,
            hendelseKey.identifikator,
            hendelseKey.gjelderPeriode,
            HendelseMetadata(pensjonsgivendeInntekt.metaData.retries)
        )
    }
}
