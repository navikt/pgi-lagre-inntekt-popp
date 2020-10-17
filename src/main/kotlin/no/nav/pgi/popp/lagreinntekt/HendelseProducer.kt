package no.nav.pgi.popp.lagreinntekt

import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_HENDELSE_TOPIC
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord

internal class HendelseProducer(kafkaConfig: KafkaConfig) {
    private val producer = KafkaProducer<HendelseKey, Hendelse>(kafkaConfig.commonConfig() + hendelseProducerConfig())

    internal fun rePublishHendelse(hendelseKey: HendelseKey) {
        val record = ProducerRecord(PGI_HENDELSE_TOPIC, hendelseKey, toHendelse(hendelseKey))
        producer.send(record).get()
    }

    private fun toHendelse(hendelseKey: HendelseKey) =
            Hendelse(-1L, hendelseKey.getIdentifikator(), hendelseKey.getGjelderPeriode())

    private fun hendelseProducerConfig() = mapOf(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to Integer.MAX_VALUE
    )
}