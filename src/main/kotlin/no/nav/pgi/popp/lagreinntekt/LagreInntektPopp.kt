package no.nav.pgi.popp.lagreinntekt

import no.nav.pensjon.samhandling.env.getVal
import no.nav.pgi.popp.lagreinntekt.kafka.HendelseProducer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.PensjonsgivendeInntektConsumer
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import no.nav.pgi.popp.lagreinntekt.popp.mapToPensjonsgivendeInntektDto
import org.slf4j.LoggerFactory
import java.io.IOException
import kotlin.system.exitProcess

internal class LagreInntektPopp(kafkaConfig: KafkaConfig = KafkaConfig(), env: Map<String, String> = System.getenv()) {
    private val consumer = PensjonsgivendeInntektConsumer(kafkaConfig)
    private val hendelseProducer = HendelseProducer(kafkaConfig)
    private val poppClient = PoppClient(env.getVal("POPP_URL"))

    internal fun start(loopForever: Boolean = true) {
        do try {
            val inntektRecords = consumer.getInntektRecords()
            inntektRecords.forEach { record ->
                val response = poppClient.postPensjonsgivendeInntekt(mapToPensjonsgivendeInntektDto(record))
                if (response.statusCode() != 201) {
                    hendelseProducer.republishHendelse(record.key())
                }
            }
            consumer.commit()
        } catch (e: IOException) {
            LOG.warn(e.message, e)
            //TODO: Håndter denne
        } catch (e: Exception) {
            LOG.error(e.message)
            e.printStackTrace()
            exitProcess(1)
        } while (loopForever)
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(LagreInntektPopp::class.java)
    }
}