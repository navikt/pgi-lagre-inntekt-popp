package no.nav.pgi.popp.lagreinntekt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.pensjon.samhandling.liveness.isAlive
import no.nav.pensjon.samhandling.liveness.isReady
import no.nav.pensjon.samhandling.metrics.metrics


fun main() {
    createApplication().apply {
        start(wait = false)
    }
}

internal fun createApplication(serverPort: Int = 8080) =
        embeddedServer(Netty, createApplicationEnvironment(serverPort))

private fun createApplicationEnvironment(serverPort: Int) =
        applicationEngineEnvironment {
            connector { port = serverPort }
            module {
                isAlive()
                isReady()
                metrics()
            }
        }