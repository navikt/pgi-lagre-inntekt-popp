package no.nav.pgi.popp.lagreinntekt

import io.prometheus.client.Counter
import net.logstash.logback.marker.Markers
import no.nav.pensjon.samhandling.maskfnr.maskFnr
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaInntektFactory
import no.nav.pgi.popp.lagreinntekt.kafka.inntekt.PensjonsgivendeInntektConsumer
import no.nav.pgi.popp.lagreinntekt.kafka.republish.RepubliserHendelseProducer
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient.PoppResponse
import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.slf4j.LoggerFactory
import java.net.http.HttpResponse
import java.util.concurrent.atomic.AtomicBoolean


internal class LagreInntektPopp(
    private val poppClient: PoppClient,
    kafkaFactory: KafkaFactory = KafkaInntektFactory()
) {
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
                LOG.info(Markers.append("sekvensnummer", inntektRecord.value().getMetaData().getSekvensnummer()), "Kaller POPP for lagring av pgi. Sekvensnummer: ${inntektRecord.value().getMetaData().getSekvensnummer()}")
                val response = poppClient.postPensjonsgivendeInntekt(inntektRecord.value())
                when {
                    response.statusCode() == 200 -> logSuccessfulRequestToPopp(response, inntektRecord.value())
                    response.statusCode() == 400 && response errorMessage "PGI_001_PID_VALIDATION_FAILED" -> logPidValidationFailed(response, inntektRecord.value())
                    response.statusCode() == 400 && response errorMessage "PGI_002_INNTEKT_AAR_VALIDATION_FAILED" -> logInntektAarValidationFailed(response, inntektRecord.value())
                    response.statusCode() == 409 -> {
                        when {
                            response errorMessage "Bruker eksisterer ikke i PEN" -> {
                                // Rettes ofte automatisk i PEN innen få dager. Logges som error i republisering ved mange retries.
                                logWarningBrukerEksistereIkkeIPenRepublishing(response, inntektRecord.value())
                                republiserHendelseProducer.send(inntektRecord)
                            }
                            else -> {
                                logErrorRepublishing(response, inntektRecord.value())
                                republiserHendelseProducer.send(inntektRecord)
                            }
                        }
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

    private fun handleInntektRecords(inntektRecords: List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>) {
        val delayRequestsToPopp = inntektRecords.hasDuplicates()
        if (delayRequestsToPopp) LOG.info("More than one of the same fnr in polled records, delaying calls to popp for ${inntektRecords.size} records")
        inntektRecords.forEach { inntektRecord ->
            handleInntektRecord(delayRequestsToPopp, inntektRecord)
        }
    }

    private fun handleInntektRecord(
        delayRequestsToPopp: Boolean,
        inntektRecord: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>
    ) {
        if (delayRequestsToPopp) Thread.sleep(30L)
        LOG.info(
            Markers.append("sekvensnummer", inntektRecord.value().getMetaData().getSekvensnummer()),
            "Kaller POPP for lagring av pgi. Sekvensnummer: ${
                inntektRecord.value().getMetaData().getSekvensnummer()
            }"
        )
        val response = poppClient.postPensjonsgivendeInntekt(inntektRecord.value())
        when (response) {
            is PoppResponse.OK -> logSuccessfulRequestToPopp(response.httpResponse, inntektRecord.value())
            is PoppResponse.PidValidationFailed -> logPidValidationFailed(
                response.httpResponse,
                inntektRecord.value()
            )

            is PoppResponse.InntektAarValidationFailed -> logInntektAarValidationFailed(
                response.httpResponse,
                inntektRecord.value()
            )

            is PoppResponse.BrukerEksistererIkkeIPEN -> {
                logRepublishingFailedInntekt(response.httpResponse, inntektRecord.value())
                republiserHendelseProducer.send(inntektRecord)
            }

            is PoppResponse.UkjentStatus -> {
                logShuttingDownDueToUnhandledStatus(response.httpResponse, inntektRecord.value())
                throw UnhandledStatusCodePoppException(response.httpResponse)
            }
        }
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
        PoppResponseCounter.ok(response.statusCode())
        val sekvensnummer = pgi.getMetaData().getSekvensnummer()
        val marker = Markers.append("sekvensnummer", sekvensnummer)
        LOG.info(
            marker,
            "Lagret OK i POPP. (Status: ${response.statusCode()}) Sekvensnummer: $sekvensnummer"
        )
        SECURE_LOG.info(
            marker,
            "Lagret OK i POPP. ${response.logString()}. For pgi: $pgi"
        )
    }

    private fun logPidValidationFailed(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        PoppResponseCounter.pidValidationFailed(response.statusCode())
        val sekvensnummer = pgi.getMetaData().getSekvensnummer()
        val marker = Markers.append("sekvensnummer", sekvensnummer)
        LOG.warn(
            marker,
            """Failed when adding to POPP. Inntekt will be descarded. Pid did not validate ${response.logString()}. For pgi: $pgi""".maskFnr()
        )
        SECURE_LOG.warn(
            marker,
            "Failed when adding to POPP. Inntekt will be descarded. Pid did not validate ${response.logString()}. For pgi: $pgi"
        )
    }

    private fun logInntektAarValidationFailed(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        PoppResponseCounter.inntektArValidation(response.statusCode())
        val sekvensnummer = pgi.getMetaData().getSekvensnummer()
        val marker = Markers.append("sekvensnummer", sekvensnummer)
        LOG.warn(
            marker,
            """Inntektaar is not valid for pgi. Inntekt will be descarded. ${response.logString()}. For pgi: $pgi """.maskFnr()
        )
        SECURE_LOG.warn(
            marker,
            "Inntektaar is not valid for pgi. Inntekt will be descarded.. ${response.logString()}. For pgi: $pgi "
        )
    }

    private fun logWarningBrukerEksistereIkkeIPenRepublishing(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        pgiPoppResponseCounter.labels("${response.statusCode()}_Republish").inc()
        LOG.warn(
            Markers.append("sekvensnummer", pgi.getMetaData().getSekvensnummer()),
            """Failed when adding to POPP. Bruker eksisterer ikke i PEN. Initiating republishing. ${response.logString()}. For pgi: $pgi""".maskFnr()
        )
        SECURE_LOG.warn(
            Markers.append("sekvensnummer", pgi.getMetaData().getSekvensnummer()),
            "Failed when adding to POPP. Bruker eksisterer ikke i PEN. Initiating republishing. ${response.logString()}. For pgi: $pgi"
        )
    }

    private fun logErrorRepublishing(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        pgiPoppRespnseCounter.labels("${response.statusCode()}_Republish").inc()
        LOG.error(Markers.append("sekvensnummer", pgi.getMetaData().getSekvensnummer()), """Failed when adding to POPP. Initiating republishing. ${response.logString()}. For pgi: $pgi""".maskFnr())
        SECURE_LOG.error(Markers.append("sekvensnummer", pgi.getMetaData().getSekvensnummer()), "Failed when adding to POPP. Initiating republishing. ${response.logString()}. For pgi: $pgi")
    }

    private fun logShuttingDownDueToUnhandledStatus(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        PoppResponseCounter.shutdown(response.statusCode())
        val sekvensnummer = pgi.getMetaData().getSekvensnummer()
        val marker = Markers.append("sekvensnummer", sekvensnummer)
        LOG.error(
            marker,
            """Failed when adding to POPP. Initiating shutdown. ${response.logString()}. For pgi: $pgi """.maskFnr()
        )
        SECURE_LOG.error(
            marker,
            "Failed when adding to POPP. Initiating shutdown. ${response.logString()}. For pgi: $pgi "
        )
    }
}

private fun List<ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>>.hasDuplicates() =
    map { it.key() }.toHashSet().size != size

private fun HttpResponse<String>.logString() =
    "PoppResponse(Status: ${statusCode()}${if (body().isEmpty()) "" else " Body: ${body()}"})"
