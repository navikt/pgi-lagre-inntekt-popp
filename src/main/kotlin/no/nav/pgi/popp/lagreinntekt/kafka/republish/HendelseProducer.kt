package no.nav.pgi.popp.lagreinntekt.kafka.republish

import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_TOPIC
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

internal class HendelseProducer(kafkaFactory: KafkaFactory) {
    private val producer: Producer<HendelseKey, Hendelse> = kafkaFactory.hendelseProducer()

    //TODO: Error handling
    internal fun republishHendelse(hendelseKey: HendelseKey) {
        val record = ProducerRecord(PGI_HENDELSE_TOPIC, hendelseKey, toHendelse(hendelseKey))
        producer.send(record).get()
        LOG.warn("Republiserer ${record.key()} to $PGI_HENDELSE_TOPIC") //TODO: Mask fnr
    }

    private fun toHendelse(hendelseKey: HendelseKey) =
            Hendelse(-1L, hendelseKey.getIdentifikator(), hendelseKey.getGjelderPeriode())

    companion object {
        private val LOG = LoggerFactory.getLogger(HendelseProducer::class.java)
    }
}