package no.nav.pgi.popp.lagreinntekt

import java.time.Duration

private val TIMEOUT_DURATION = Duration.ofSeconds(4)

internal class InntektConsumer(kafkaConfig: KafkaConfig) {
    private val consumer = kafkaConfig.inntektConsumer()

    init {
        consumer.subscribe(listOf(KafkaConfig.PGI_INNTEKT_TOPIC))
    }

    internal fun getInntekter() = consumer.poll(TIMEOUT_DURATION).records(KafkaConfig.PGI_INNTEKT_TOPIC).toList()

}
