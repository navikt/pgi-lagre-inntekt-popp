package no.nav.pgi.popp.lagreinntekt

import io.prometheus.client.Counter
import no.nav.pensjon.samhandling.maskfnr.maskFnr
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaInntektFactory
import no.nav.pgi.popp.lagreinntekt.kafka.inntekt.PensjonsgivendeInntektConsumer
import no.nav.pgi.popp.lagreinntekt.kafka.republish.RepubliserHendelseProducer
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.slf4j.LoggerFactory
import java.net.http.HttpResponse
import java.util.concurrent.atomic.AtomicBoolean

private val pgiPoppRespnseCounter = Counter.build()
    .name("pgi_lagre_inntekt_popp_response_counter")
    .labelNames("statusCode")
    .help("Count response status codes from popp")
    .register()

internal class LagreInntektPopp(private val poppClient: PoppClient, kafkaFactory: KafkaFactory = KafkaInntektFactory()) {
    private val pgiConsumer = PensjonsgivendeInntektConsumer(kafkaFactory)
    private val republiserHendelseProducer = RepubliserHendelseProducer(kafkaFactory)

    private var stop: AtomicBoolean = AtomicBoolean(false)

    private companion object {
        private val LOG = LoggerFactory.getLogger(LagreInntektPopp::class.java)
        private val SECURE_LOG = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun start(loopForever: Boolean = true) {
        do try {
            val inntektRecords: List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>> =
                pgiConsumer.pollInntektRecords()
            val delayRequestsToPopp = inntektRecords.isDuplicates()
            if (delayRequestsToPopp) LOG.info("More than one of the same fnr in polled records, delaying calls to popp for ${inntektRecords.size} records")
            inntektRecords.forEach { inntektRecord ->
                if (delayRequestsToPopp) Thread.sleep(30L)
                LOG.info("Kaller POPP for lagring av pgi. {\"sekvensnummer\": ${inntektRecord.value().getMetaData().getSekvensnummer()}}")
                val response = poppClient.postPensjonsgivendeInntekt(inntektRecord.value())
                when {
                    response.statusCode() == 200 -> logSuccessfulRequestToPopp(response, inntektRecord.value())
                    response.statusCode() == 400 && response errorMessage "PGI_001_PID_VALIDATION_FAILED" -> logPidValidationFailed(response, inntektRecord.value())
                    response.statusCode() == 400 && response errorMessage "PGI_002_INNTEKT_AAR_VALIDATION_FAILED" -> logInntektAarValidationFailed(response, inntektRecord.value())
                    response.statusCode() == 409 -> {
                        logRepublishingFailedInntekt(response, inntektRecord.value())
                        republiserHendelseProducer.send(inntektRecord)
                    }
                    else -> {
                        logShuttingDownDueToUnhandledStatus(response, inntektRecord.value())
                        throw UnhandledStatusCodePoppException(response)
                    }
                }
            }
            pgiConsumer.commit()
        } catch (topicAuthorizationException: TopicAuthorizationException) {
            LOG.warn("Kafka credential rotation triggered. Shutting down app")
            throw topicAuthorizationException
        } while (loopForever && !stop.get())
    }

    internal fun stop() {
        LOG.info("stopping LagreInntektPopp")
        stop.set(true)
    }

    internal fun isClosed() = pgiConsumer.isClosed() && republiserHendelseProducer.isClosed()

    internal fun closeKafka() {
        pgiConsumer.close()
        republiserHendelseProducer.close()
    }

    private fun logSuccessfulRequestToPopp(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        pgiPoppRespnseCounter.labels("${response.statusCode()}_OK").inc()
        LOG.info("Lagret OK i POPP. (Status: ${response.statusCode()}) {\"sekvensnummer\": ${pgi.getMetaData().getSekvensnummer()}}")
        SECURE_LOG.info("Lagret OK i POPP. ${response.logString()}. For pgi: $pgi")
    }

    private fun logPidValidationFailed(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        pgiPoppRespnseCounter.labels("${response.statusCode()}_Pid_Validation").inc()
        LOG.warn("""Failed when adding to POPP. Inntekt will be descarded. Pid did not validate ${response.logString()}. For pgi: $pgi""".maskFnr())
        SECURE_LOG.warn("Failed when adding to POPP. Inntekt will be descarded. Pid did not validate ${response.logString()}. For pgi: $pgi")
    }

    private fun logInntektAarValidationFailed(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt?) {
        pgiPoppRespnseCounter.labels("${response.statusCode()}_InnntektAar_Validation").inc()
        LOG.info("""Inntektaar is not valid for pgi. Inntekt will be descarded. ${response.logString()}. For pgi: $pgi """.maskFnr())
        SECURE_LOG.info("Inntektaar is not valid for pgi. Inntekt will be descarded.. ${response.logString()}. For pgi: $pgi ")
    }

    private fun logRepublishingFailedInntekt(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        pgiPoppRespnseCounter.labels("${response.statusCode()}_Republish").inc()
        LOG.warn("""Failed when adding to POPP. Initiating republishing. ${response.logString()}. For pgi: $pgi""".maskFnr())
        SECURE_LOG.warn("Failed when adding to POPP. Initiating republishing. ${response.logString()}. For pgi: $pgi")
    }

    private fun logShuttingDownDueToUnhandledStatus(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        pgiPoppRespnseCounter.labels("${response.statusCode()}_ShutDown").inc()
        LOG.error("""Failed when adding to POPP. Initiating shutdown. ${response.logString()}. For pgi: $pgi """.maskFnr())
        SECURE_LOG.error("Failed when adding to POPP. Initiating shutdown. ${response.logString()}. For pgi: $pgi ")
    }
}

internal class UnhandledStatusCodePoppException(response: HttpResponse<String>) :
    Exception("""Unhandled status code in PoppResponse(Status: ${response.statusCode()} Body: ${response.body()})""".maskFnr())

private fun List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>.isDuplicates() = map { it.key() }.toHashSet().size != size

private infix fun HttpResponse<String>.errorMessage(errorMessage: String) = body().contains(errorMessage)

private fun HttpResponse<String>.logString() = "PoppResponse(Status: ${statusCode()}${if(body().isEmpty())"" else " Body: ${body()}"})"
