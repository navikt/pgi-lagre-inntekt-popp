package no.nav.pgi.popp.lagreinntekt

import io.prometheus.client.Counter

object PoppResponseCounter {
    private val counter = Counter.build()
        .name("pgi_lagre_inntekt_popp_response_counter")
        .labelNames("statusCode")
        .help("Count response status codes from popp")
        .register()

    fun ok(statusCode: Int) {
        counter.labels("${statusCode}_OK").inc()
    }

    fun pidValidationFailed(statusCode: Int) {
        counter.labels("${statusCode}_Pid_Validation").inc()
    }

    fun inntektArValidation(statusCode: Int) {
        counter.labels("${statusCode}_InnntektAar_Validation").inc()
    }

    fun republish(statusCode: Int) {
        counter.labels("${statusCode}_Republish").inc()
    }

    fun shutdown(statusCode: Int) {
        counter.labels("${statusCode}_Shutdown").inc()
    }
}
