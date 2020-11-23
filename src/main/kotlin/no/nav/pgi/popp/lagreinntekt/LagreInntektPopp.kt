package no.nav.pgi.popp.lagreinntekt

import no.nav.pensjon.samhandling.env.getVal
import no.nav.pgi.popp.lagreinntekt.kafka.HendelseProducer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaConfig
import no.nav.pgi.popp.lagreinntekt.kafka.PensjonsgivendeInntektConsumer
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.http.HttpResponse
import kotlin.system.exitProcess

internal class LagreInntektPopp(kafkaConfig: KafkaConfig = KafkaConfig(), env: Map<String, String> = System.getenv()) {
    private val consumer = PensjonsgivendeInntektConsumer(kafkaConfig)
    private val hendelseProducer = HendelseProducer(kafkaConfig)
    private val poppClient = PoppClient(env.getVal("POPP_URL"))

    internal fun start(loopForever: Boolean = true) {
        do try {
            val inntektRecords = consumer.getInntektRecords()
            inntektRecords.forEach { inntektRecord ->

                val response = poppClient.postPensjonsgivendeInntekt(inntektRecord.value())
                if (response.statusCode() != 201) {
                    logFailedInntektToPopp(inntektRecord, response)
                    hendelseProducer.republishHendelse(inntektRecord.key())
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

    private fun logFailedInntektToPopp(inntektRecord: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>?, response: HttpResponse<String>) {
        LOG.warn("$inntektRecord could not be sent to popp. Status code: ${response.statusCode()}. Body: ${response.body()}")
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(LagreInntektPopp::class.java)
    }
}