package no.nav.pgi.popp.lagreinntekt.kafka

import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

internal class HendelseProducer(kafkaConfig: KafkaConfig) {
    private val producer = KafkaProducer<HendelseKey, Hendelse>(kafkaConfig.commonConfig() + hendelseProducerConfig())

    internal fun rePublishHendelser(inntekterFeiletTilPopp: MutableList<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>) {
        inntekterFeiletTilPopp.forEach {
            rePublishHendelse(it.key())
        }.also { log.warn("Republiserer ${inntekterFeiletTilPopp.size} hendelse(r) til topic $PGI_HENDELSE_TOPIC.") }
    }

    private fun rePublishHendelse(hendelseKey: HendelseKey) {
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

    companion object {
        private val log = LoggerFactory.getLogger(HendelseProducer::class.java)
    }
}