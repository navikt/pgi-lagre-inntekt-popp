package no.nav.pgi.popp.lagreinntekt.kafka.republish

import no.nav.pensjon.samhandling.maskfnr.maskFnr
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_TOPIC
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.HendelseMetadata
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.jvm.internal.impl.resolve.constants.LongValue

internal class HendelseProducer(kafkaFactory: KafkaFactory) {
    private val producer: Producer<HendelseKey, Hendelse> = kafkaFactory.hendelseProducer()
    private var closed: AtomicBoolean = AtomicBoolean(false)

    internal fun republishHendelse(consumerRecord: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>) {
        val hendelseRecord = ProducerRecord(PGI_HENDELSE_TOPIC, consumerRecord.key(), toHendelse(consumerRecord))
        producer.send(hendelseRecord).get()
        LOG.warn("Republiserer ${hendelseRecord.value()} to $PGI_HENDELSE_TOPIC".maskFnr())
    }

    internal fun close() {
        LOG.info("Closing ${HendelseProducer::class.simpleName}")
        producer.close()
        closed.set(true)
    }

    internal fun isClosed() = closed.get()

    private fun toHendelse(consumerRecord: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>): Hendelse {
        return Hendelse(
            consumerRecord.value().getMetaData().getSekvensnummer(),
            consumerRecord.key().getIdentifikator(),
            consumerRecord.key().getGjelderPeriode(),
            HendelseMetadata(incrementRetries(consumerRecord))
        )
    }

    private fun incrementRetries(consumerRecord: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>) =
        consumerRecord.value().getMetaData().getRetries() + 1

    companion object {
        private val LOG = LoggerFactory.getLogger(HendelseProducer::class.java)
    }
}