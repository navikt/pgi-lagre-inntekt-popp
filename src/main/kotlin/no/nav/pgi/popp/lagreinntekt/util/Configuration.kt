package no.nav.pgi.popp.lagreinntekt.util

import io.micrometer.core.instrument.MeterRegistry
import no.nav.pgi.popp.lagreinntekt.ApplicationService
import no.nav.pgi.popp.lagreinntekt.ApplicationStatus
import no.nav.pgi.popp.lagreinntekt.Counters
import no.nav.pgi.popp.lagreinntekt.PoppResponseCounter
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import kotlin.system.exitProcess

@Configuration
@EnableScheduling
@Profile("dev-gcp", "prod-gcp")
class Configuration {

    @Bean
    fun applicationService(
        meterRegistry: MeterRegistry,
    ): ApplicationService {
        LOG.info("Created ApplicationService")
        return ApplicationService(
            applicationStatus = ApplicationStatus(),
            poppResponseCounter = PoppResponseCounter(Counters(meterRegistry)),
            exitApplication = {
                LOG.info("Exiting through injected callback")
                exitProcess(0)
            }
        )
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(Configuration::class.java)
        private val SECURE_LOG = LoggerFactory.getLogger("tjenestekall")
    }

}
