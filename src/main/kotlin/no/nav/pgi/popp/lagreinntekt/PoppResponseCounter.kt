package no.nav.pgi.popp.lagreinntekt

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

class PoppResponseCounter(private val meterRegistry: MeterRegistry) {

    private fun poppResponses(statusCode: String) : Counter {
        return meterRegistry.counter(
            "pgi_lagre_inntekt_popp_response_counter",
            listOf(
                Tag.of("statusCode", statusCode),
                Tag.of("help", "Count response status codes from popp")
            )
        )
    }

    fun ok(statusCode: Int) {
        poppResponses("${statusCode}_OK").increment()
    }

    fun pidValidationFailed(statusCode: Int) {
        poppResponses("${statusCode}_Pid_Validation").increment()
    }

    fun inntektArValidation(statusCode: Int) {
        poppResponses("${statusCode}_InnntektAar_Validation").increment()
    }

    fun republish(statusCode: Int) {
        poppResponses("${statusCode}_Republish").increment()
    }

    fun shutdown(statusCode: Int) {
        poppResponses("${statusCode}_Shutdown").increment()
    }
}
