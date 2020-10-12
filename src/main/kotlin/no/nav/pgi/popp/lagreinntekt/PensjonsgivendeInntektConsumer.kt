package no.nav.pgi.popp.lagreinntekt

import java.time.Duration

private val TIMEOUT_DURATION = Duration.ofSeconds(4)

internal class PensjonsgivendeInntektConsumer(kafkaConfig: KafkaConfig) {
    private val consumer = kafkaConfig.inntektConsumer()

    init { consumer.subscribe(listOf(PGI_INNTEKT_TOPIC)) }

    internal fun getInntekter() = consumer.poll(TIMEOUT_DURATION).records(PGI_INNTEKT_TOPIC).toList()
}
