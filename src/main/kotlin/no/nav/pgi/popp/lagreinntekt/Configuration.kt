package no.nav.pgi.popp.lagreinntekt

import io.micrometer.core.instrument.MeterRegistry
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
        log.info("Created ApplicationService")
        return ApplicationService(
            applicationStatus = ApplicationStatus(),
            poppResponseCounter = PoppResponseCounter(meterRegistry),
            exitApplication = {
                log.info("Exiting through injected callback")
                exitProcess(0)
            }
        )
    }

    private companion object {
        private val log = LoggerFactory.getLogger(Configuration::class.java)
        private val secureLog = LoggerFactory.getLogger("tjenestekall")
    }

}
