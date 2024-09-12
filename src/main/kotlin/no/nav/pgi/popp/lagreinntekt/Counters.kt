package no.nav.pgi.popp.lagreinntekt

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.util.concurrent.atomic.AtomicLong

class Counters(private val meterRegistry: MeterRegistry) {

    private val persistedSekvensnummer = AtomicLong(0)

    private fun poppResponses(statusCode: String) : Counter {
        return meterRegistry.counter(
            "pgi_lagre_inntekt_popp_response_counter",
            listOf(
                Tag.of("statusCode", statusCode),
                Tag.of("help", "Count response status codes from popp")
            )
        )
    }

    private val hendelserFailedToTopic =
        meterRegistry.counter(
            "pgi_hendelser_failed_to_topic",
            listOf(
                Tag.of("help", "Antall hendelser som feilet når de skulle legges til topic eller vil bli overskrevet")
            )
        )

    private val persistedSekvensnummerGauge = meterRegistry.gauge(
        "persistedSekvensnummer",
        listOf(Tag.of("help", "Siste persisterte som brukes når det hentes pgi-hendelser fra skatt")),
        persistedSekvensnummer
    )

    fun incrementPoppResponse(statusCode: String) {
        poppResponses(statusCode).increment()
    }

    fun setPersistredSekvensnummer(sekvensnummer: Long) {
        persistedSekvensnummer.set(sekvensnummer)
    }
}