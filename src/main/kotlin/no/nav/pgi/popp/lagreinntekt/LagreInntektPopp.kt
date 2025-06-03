package no.nav.pgi.popp.lagreinntekt

import net.logstash.logback.marker.Markers
import no.nav.pgi.domain.PensjonsgivendeInntekt
import no.nav.pgi.domain.serialization.PgiDomainSerializer
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaFactory
import no.nav.pgi.popp.lagreinntekt.kafka.KafkaInntektFactory
import no.nav.pgi.popp.lagreinntekt.kafka.inntekt.PensjonsgivendeInntektConsumer
import no.nav.pgi.popp.lagreinntekt.kafka.republish.RepubliserHendelseProducer
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient.PoppResponse
import no.nav.pgi.popp.lagreinntekt.util.maskFnr
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.slf4j.LoggerFactory
import java.net.http.HttpResponse


internal class LagreInntektPopp(
    private val poppResponseCounter: PoppResponseCounter,
    private val poppClient: PoppClient,
    kafkaFactory: KafkaFactory = KafkaInntektFactory()
) {
    private val pgiConsumer = PensjonsgivendeInntektConsumer(kafkaFactory)
    private val republiserHendelseProducer = RepubliserHendelseProducer(kafkaFactory)

    private companion object {
        private val log = LoggerFactory.getLogger(LagreInntektPopp::class.java)
        private val secureLog = LoggerFactory.getLogger("team")
    }

    internal fun processInntektRecords() {
        try {
            val inntektRecords = pgiConsumer.pollInntektRecords()
            handleInntektRecords(inntektRecords)
            pgiConsumer.commit()
        } catch (topicAuthorizationException: TopicAuthorizationException) {
            log.warn("Kafka credential rotation triggered. Shutting down app")
            throw topicAuthorizationException
        }
    }

    private fun handleInntektRecords(inntektRecords: List<ConsumerRecord<String, String>>) {
        val delayRequestsToPopp = inntektRecords.hasDuplicates()
        if (delayRequestsToPopp) log.info("More than one of the same fnr in polled records, delaying calls to popp for ${inntektRecords.size} records")
        inntektRecords.forEach { inntektRecord ->
            handleInntektRecord(delayRequestsToPopp, inntektRecord)
        }
    }

    private fun handleInntektRecord(
        delayRequestsToPopp: Boolean,
        inntektRecord: ConsumerRecord<String, String>
    ) {
        if (delayRequestsToPopp) {
            Thread.sleep(30L)
        }
        val pensjonsgivendeInntekt =
            PgiDomainSerializer().fromJson(PensjonsgivendeInntekt::class, inntektRecord.value())
        log.info(
            Markers.append("sekvensnummer", pensjonsgivendeInntekt.metaData.sekvensnummer),
            "Kaller POPP for lagring av pgi. Sekvensnummer: ${pensjonsgivendeInntekt.metaData.sekvensnummer}"
        )
        when (val response = poppClient.postPensjonsgivendeInntekt(pensjonsgivendeInntekt)) {
            is PoppResponse.OK -> logSuccessfulRequestToPopp(response.httpResponse, pensjonsgivendeInntekt)
            is PoppResponse.PidValidationFailed -> logPidValidationFailed(
                response.httpResponse,
                pensjonsgivendeInntekt
            )

            is PoppResponse.InntektAarValidationFailed -> logInntektAarValidationFailed(
                response.httpResponse,
                pensjonsgivendeInntekt
            )

            is PoppResponse.BrukerEksistererIkkeIPEN -> {
                logWarningBrukerEksistereIkkeIPenRepublishing(response.httpResponse, pensjonsgivendeInntekt)
                republiserHendelseProducer.send(inntektRecord)
            }

            is PoppResponse.AnnenKonflikt -> {
                logErrorRepublishing(response.httpResponse, pensjonsgivendeInntekt)
                republiserHendelseProducer.send(inntektRecord)
            }

            is PoppResponse.UkjentStatus -> {
                logShuttingDownDueToUnhandledStatus(response.httpResponse, pensjonsgivendeInntekt)
                throw UnhandledStatusCodePoppException(response.httpResponse)
            }
        }
    }

    internal fun isClosed() = pgiConsumer.isClosed() && republiserHendelseProducer.isClosed()

    internal fun closeKafka() {
        pgiConsumer.close()
        republiserHendelseProducer.close()
    }

    private fun logSuccessfulRequestToPopp(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        poppResponseCounter.ok(response.statusCode())
        val sekvensnummer = pgi.metaData.sekvensnummer
        val marker = Markers.append("sekvensnummer", sekvensnummer)
        log.info(
            marker,
            "Lagret OK i POPP. (Status: ${response.statusCode()}) Sekvensnummer: $sekvensnummer"
        )
        secureLog.info(
            marker,
            "Lagret OK i POPP. ${response.logString()}. For pgi: $pgi"
        )
    }

    private fun logPidValidationFailed(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        poppResponseCounter.pidValidationFailed(response.statusCode())
        val sekvensnummer = pgi.metaData.sekvensnummer
        val marker = Markers.append("sekvensnummer", sekvensnummer)
        log.warn(
            marker,
            """Failed when adding to POPP. Inntekt will be descarded. Pid did not validate ${response.logString()}. For pgi: $pgi""".maskFnr()
        )
        secureLog.warn(
            marker,
            "Failed when adding to POPP. Inntekt will be descarded. Pid did not validate ${response.logString()}. For pgi: $pgi"
        )
    }

    private fun logInntektAarValidationFailed(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        poppResponseCounter.inntektArValidation(response.statusCode())
        val sekvensnummer = pgi.metaData.sekvensnummer
        val marker = Markers.append("sekvensnummer", sekvensnummer)
        log.warn(
            marker,
            """Inntektaar is not valid for pgi. Inntekt will be descarded. ${response.logString()}. For pgi: $pgi """.maskFnr()
        )
        secureLog.warn(
            marker,
            "Inntektaar is not valid for pgi. Inntekt will be descarded.. ${response.logString()}. For pgi: $pgi "
        )
    }

    private fun logWarningBrukerEksistereIkkeIPenRepublishing(
        response: HttpResponse<String>,
        pgi: PensjonsgivendeInntekt
    ) {
        poppResponseCounter.republish(response.statusCode())
        log.warn(
            Markers.append("sekvensnummer", pgi.metaData.sekvensnummer),
            """Failed when adding to POPP. Bruker eksisterer ikke i PEN. Initiating republishing. ${response.logString()}. For pgi: $pgi""".maskFnr()
        )
        secureLog.warn(
            Markers.append("sekvensnummer", pgi.metaData.sekvensnummer),
            "Failed when adding to POPP. Bruker eksisterer ikke i PEN. Initiating republishing. ${response.logString()}. For pgi: $pgi"
        )
    }

    private fun logErrorRepublishing(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        poppResponseCounter.republish(response.statusCode())
        log.error(
            Markers.append("sekvensnummer", pgi.metaData.sekvensnummer),
            """Failed when adding to POPP. Initiating republishing. ${response.logString()}. For pgi: $pgi""".maskFnr()
        )
        secureLog.error(
            Markers.append("sekvensnummer", pgi.metaData.sekvensnummer),
            "Failed when adding to POPP. Initiating republishing. ${response.logString()}. For pgi: $pgi"
        )
    }

    private fun logShuttingDownDueToUnhandledStatus(response: HttpResponse<String>, pgi: PensjonsgivendeInntekt) {
        poppResponseCounter.shutdown(response.statusCode())
        val sekvensnummer = pgi.metaData.sekvensnummer
        val marker = Markers.append("sekvensnummer", sekvensnummer)
        log.error(
            marker,
            """Failed when adding to POPP. Initiating shutdown. ${response.logString()}. For pgi: $pgi """.maskFnr()
        )
        secureLog.error(
            marker,
            "Failed when adding to POPP. Initiating shutdown. ${response.logString()}. For pgi: $pgi "
        )
    }
}

private fun List<ConsumerRecord<String, String>>.hasDuplicates() =
    map { it.key() }.toHashSet().size != size

private fun HttpResponse<String>.logString() =
    "PoppResponse(Status: ${statusCode()}${if (body().isEmpty()) "" else " Body: ${body()}"})"
