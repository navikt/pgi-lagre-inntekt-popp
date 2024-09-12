package no.nav.pgi.popp.lagreinntekt

class PoppResponseCounter(private val counters: Counters) {

    fun ok(statusCode: Int) {
        counters.incrementPoppResponse("${statusCode}_OK")
    }

    fun pidValidationFailed(statusCode: Int) {
        counters.incrementPoppResponse("${statusCode}_Pid_Validation")
    }

    fun inntektArValidation(statusCode: Int) {
        counters.incrementPoppResponse("${statusCode}_InnntektAar_Validation")
    }

    fun republish(statusCode: Int) {
        counters.incrementPoppResponse("${statusCode}_Republish")
    }

    fun shutdown(statusCode: Int) {
        counters.incrementPoppResponse("${statusCode}_Shutdown")
    }
}
