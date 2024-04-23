package no.nav.pgi.popp.lagreinntekt.kafka.republish

import net.logstash.logback.marker.Markers
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_REPUBLISERING_TOPIC
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.HendelseMetadata
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

internal class RepubliserHendelseProducer(kafkaFactory: KafkaFactory) {
    private val producer: Producer<HendelseKey, Hendelse> = kafkaFactory.hendelseProducer()
    private var closed: AtomicBoolean = AtomicBoolean(false)

    companion object {
        private val LOG = LoggerFactory.getLogger(RepubliserHendelseProducer::class.java)
        private val SECURE_LOG = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun send(consumerRecord: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>) {
        val hendelseRecord =
            ProducerRecord(PGI_HENDELSE_REPUBLISERING_TOPIC, consumerRecord.key(), toHendelse(consumerRecord))
        producer.send(hendelseRecord).get()
        LOG.info(Markers.append("sekvensnummer", hendelseRecord.value().getSekvensnummer()),"Republiserer hendelse. Sekvensnummer: ${hendelseRecord.value().getSekvensnummer()} Retries: ${hendelseRecord.value().getMetaData().getRetries()}")
        SECURE_LOG.info(Markers.append("sekvensnummer", hendelseRecord.value().getSekvensnummer()), "Republiserer hendelse. Sekvensnummer: ${hendelseRecord.value().getSekvensnummer()} Retries: ${hendelseRecord.value().getMetaData().getRetries()} Values: ${hendelseRecord.value()} to $PGI_HENDELSE_REPUBLISERING_TOPIC")
    }

    internal fun close() {
        LOG.info("Closing ${RepubliserHendelseProducer::class.simpleName}")
        producer.close()
        closed.set(true)
    }

    internal fun isClosed() = closed.get()

    private fun toHendelse(consumerRecord: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>): Hendelse {
        return Hendelse(
            consumerRecord.value().getMetaData().getSekvensnummer(),
            consumerRecord.key().getIdentifikator(),
            consumerRecord.key().getGjelderPeriode(),
            HendelseMetadata(consumerRecord.value().getMetaData().getRetries())
        )
    }
}
