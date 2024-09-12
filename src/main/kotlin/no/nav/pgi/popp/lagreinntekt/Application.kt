package no.nav.pgi.popp.lagreinntekt

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

fun main(args: Array<String>) {
    runApplication<Application>(*args)
    serviceMain()
}

@SpringBootApplication
class Application