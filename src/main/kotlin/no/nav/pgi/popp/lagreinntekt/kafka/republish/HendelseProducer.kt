package no.nav.pgi.popp.lagreinntekt.kafka.republish

import no.nav.pensjon.samhandling.maskfnr.maskFnr
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_TOPIC
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

internal class HendelseProducer(kafkaFactory: KafkaFactory) {
    private val producer: Producer<HendelseKey, Hendelse> = kafkaFactory.hendelseProducer()
    private var closed: AtomicBoolean = AtomicBoolean(false)

    internal fun republishHendelse(hendelseKey: HendelseKey) {
        val record = ProducerRecord(PGI_HENDELSE_TOPIC, hendelseKey, toHendelse(hendelseKey))
        producer.send(record).get()
        LOG.warn("Republiserer ${record.key()} to $PGI_HENDELSE_TOPIC".maskFnr())
    }

    internal fun close() {
        LOG.info("closing ${HendelseProducer::class.simpleName}")
        producer.close()
        closed.set(true)
    }

    internal fun isClosed() = closed.get()

    private fun toHendelse(hendelseKey: HendelseKey) =
            Hendelse(-1L, hendelseKey.getIdentifikator(), hendelseKey.getGjelderPeriode())

    companion object {
        private val LOG = LoggerFactory.getLogger(HendelseProducer::class.java)
    }
}