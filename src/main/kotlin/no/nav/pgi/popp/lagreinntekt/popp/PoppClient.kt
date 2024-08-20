package no.nav.pgi.popp.lagreinntekt.popp

import no.nav.pensjon.samhandling.env.getVal
import no.nav.pgi.popp.lagreinntekt.popp.LagrePgiRequestMapper.toLagrePgiRequest
import no.nav.pgi.popp.lagreinntekt.popp.token.AadTokenClient
import no.nav.pgi.popp.lagreinntekt.popp.token.AadTokenClient.AadToken
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.ws.rs.core.UriBuilder

internal const val PGI_PATH = "/popp/api/inntekt/pgi"

internal class PoppClient(
    environment: Map<String, String>,
    private val tokenProvider: TokenProvider = AadTokenClient(environment)
) {
    private val httpClient: HttpClient = HttpClient.newHttpClient()
    private val poppHost = environment.getVal(POPP_URL)
    private val poppUrl = UriBuilder.fromPath(poppHost).path(PGI_PATH).build()

    internal fun postPensjonsgivendeInntekt(pgi: PensjonsgivendeInntekt): PoppResponse {
        val response = httpClient.send(
            createPostRequest(poppUrl, toLagrePgiRequest(pgi), pgi.getMetaData().getSekvensnummer()),
            HttpResponse.BodyHandlers.ofString()
        )
        return PoppResponse.of(response)
    }


    private fun createPostRequest(poppUrl: URI, lagrePgiRequest: LagrePgiRequest, callId: Long) =
        HttpRequest.newBuilder()
            .uri(poppUrl)
            .header("Authorization", "Bearer ${tokenProvider.getToken().accessToken}")
            .header("Content-Type", "application/json")
            .header("Nav-Call-Id", callId.toString())
            .header("Nav-Consumer-Id", "pgi-lagre-inntekt-popp")
            .POST(HttpRequest.BodyPublishers.ofString(lagrePgiRequest.toJson()))
            .build()

    internal interface TokenProvider {
        fun getToken(): AadToken
    }

    private companion object EnvironmentKey {
        private const val POPP_URL = "POPP_URL"
    }

    sealed class PoppResponse() {
        data class OK(val httpResponse: HttpResponse<String>) : PoppResponse()
        data class PidValidationFailed(val httpResponse: HttpResponse<String>) : PoppResponse()
        data class InntektAarValidationFailed(val httpResponse: HttpResponse<String>) : PoppResponse()
        data class BrukerEksistererIkkeIPEN(val httpResponse: HttpResponse<String>) : PoppResponse()
        data class AnnenKonflikt(val httpResponse: HttpResponse<String>) : PoppResponse()

        data class UkjentStatus(val httpResponse: HttpResponse<String>) : PoppResponse()

        companion object {
            fun of(httpResponse: HttpResponse<String>): PoppResponse {
                val code = httpResponse.statusCode()
                fun body(msg: String) = httpResponse.body().contains(msg)
                return when {
                    code == 200 -> OK(httpResponse)
                    code == 400 && body("PGI_001_PID_VALIDATION_FAILED") -> PidValidationFailed(httpResponse)
                    code == 400 && body("PGI_002_INNTEKT_AAR_VALIDATION_FAILED") -> InntektAarValidationFailed(httpResponse)
                    code == 409 && body("Bruker eksisterer ikke i PEN") -> BrukerEksistererIkkeIPEN(httpResponse)
                    code == 409 -> AnnenKonflikt(httpResponse)
                    else -> UkjentStatus(httpResponse)
                }
            }
        }
    }

}