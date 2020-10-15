package no.nav.pgi.popp.lagreinntekt

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

const val IS_ALIVE_PATH = "/isAlive"
const val IS_READY_PATH = "/isReady"

internal fun Application.liveness() {
    routing {
        probeRouting(IS_ALIVE_PATH)
        probeRouting(IS_READY_PATH)
    }
}

private fun Routing.probeRouting(path: String) {
    get(path) {
        with("" to HttpStatusCode.OK) {
            call.respondText(first, ContentType.Text.Plain, second)
        }
    }
}