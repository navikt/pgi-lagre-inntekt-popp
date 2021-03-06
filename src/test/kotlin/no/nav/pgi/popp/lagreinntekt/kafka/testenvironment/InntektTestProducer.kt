package no.nav.pgi.popp.lagreinntekt.kafka.testenvironment

import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord

internal class InntektTestProducer(commonKafkaConfig: Map<String, String>) {

    private val inntektTestProducer = KafkaProducer<HendelseKey, PensjonsgivendeInntekt>(commonKafkaConfig + inntektTestProducerConfig())

    internal fun produceToInntektTopic(hendelseKey: HendelseKey, pensjonsgivendeInntekt: PensjonsgivendeInntekt) {
        val record = ProducerRecord(PGI_INNTEKT_TOPIC, hendelseKey, pensjonsgivendeInntekt)
        inntektTestProducer.send(record).get()
    }

    internal fun close() {
        inntektTestProducer.close()
    }

    private fun inntektTestProducerConfig() = mapOf(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to Integer.MAX_VALUE
    )
}