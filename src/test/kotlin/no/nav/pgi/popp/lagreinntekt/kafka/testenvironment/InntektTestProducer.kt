package no.nav.pgi.popp.lagreinntekt.kafka.testenvironment

import no.nav.pgi.domain.HendelseKey
import no.nav.pgi.domain.PensjonsgivendeInntekt
import no.nav.pgi.domain.serialization.PgiDomainSerializer
import no.nav.pgi.popp.lagreinntekt.kafka.PGI_INNTEKT_TOPIC
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

internal class InntektTestProducer(commonKafkaConfig: Map<String, String>) {

    private val inntektTestProducer = KafkaProducer<String, String>(commonKafkaConfig + inntektTestProducerConfig())

    internal fun produceToInntektTopic(hendelseKey: HendelseKey, pensjonsgivendeInntekt: PensjonsgivendeInntekt) {
        val key = PgiDomainSerializer().toJson(hendelseKey)
        val value = PgiDomainSerializer().toJson(pensjonsgivendeInntekt)
        val record = ProducerRecord(PGI_INNTEKT_TOPIC, key, value)
        inntektTestProducer.send(record).get()
    }

    internal fun close() {
        inntektTestProducer.close()
    }

    private fun inntektTestProducerConfig() = mapOf(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to Integer.MAX_VALUE
    )
}