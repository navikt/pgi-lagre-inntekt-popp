package no.nav.pgi.popp.lagreinntekt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.pensjon.samhandling.liveness.isAlive
import no.nav.pensjon.samhandling.liveness.isReady
import no.nav.pensjon.samhandling.metrics.metrics

internal object ReadinessServer {
    private val server = embeddedServer(Netty, createApplicationEnvironment())

    init {
        server.addShutdownHook { stopServer() }
    }

    private fun createApplicationEnvironment(serverPort: Int = 8080) =
            applicationEngineEnvironment {
                connector { port = serverPort }
                module {
                    isAlive()
                    isReady()
                    metrics()
                }
            }

    internal fun start() {
        server.start(wait = false)
    }

    private fun stopServer() {
        server.stop(100, 100)
    }
}