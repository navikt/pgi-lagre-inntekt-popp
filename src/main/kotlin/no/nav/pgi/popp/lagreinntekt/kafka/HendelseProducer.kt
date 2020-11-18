package no.nav.pgi.popp.lagreinntekt.kafka

import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

internal class HendelseProducer(kafkaConfig: KafkaConfig) {
    private val producer = KafkaProducer<HendelseKey, Hendelse>(kafkaConfig.commonConfig() + hendelseProducerConfig())

    internal fun republishHendelse(hendelseKey: HendelseKey) {
        val record = ProducerRecord(PGI_HENDELSE_TOPIC, hendelseKey, toHendelse(hendelseKey))
        producer.send(record).get()
        LOG.warn("Republiserer ${record.key()} to $PGI_HENDELSE_TOPIC") //TODO: Mask fnr
    }

    private fun toHendelse(hendelseKey: HendelseKey) =
            Hendelse(-1L, hendelseKey.getIdentifikator(), hendelseKey.getGjelderPeriode())

    private fun hendelseProducerConfig() = mapOf(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to Integer.MAX_VALUE
    )

    companion object {
        private val LOG = LoggerFactory.getLogger(HendelseProducer::class.java)
    }
}