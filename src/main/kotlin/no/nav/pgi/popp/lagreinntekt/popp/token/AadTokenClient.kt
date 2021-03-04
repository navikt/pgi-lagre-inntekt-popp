package no.nav.pgi.popp.lagreinntekt.popp.token

import com.microsoft.aad.msal4j.ClientCredentialFactory
import com.microsoft.aad.msal4j.ClientCredentialParameters
import com.microsoft.aad.msal4j.ConfidentialClientApplication
import no.nav.pensjon.samhandling.env.getVal
import no.nav.pgi.popp.lagreinntekt.popp.PoppClient.TokenProvider
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId

private val LOG = LoggerFactory.getLogger(AadTokenClient::class.java)

internal class AadTokenClient(environment: Map<String, String>) : TokenProvider {
    private val clientId = environment.getVal(CLIENT_ID)
    private val clientPassword = environment.getVal(CLIENT_PASSWORD)
    private val targetApiId = environment.getVal(TARGET_API_ID)
    private val authorityUrl = environment.getVal(WELL_KNOWN_URL)
    private val scopes = setOf("api://$targetApiId/.default")
    private val clientSecret = ClientCredentialFactory.createFromSecret(clientPassword)
    private val confidentialClientApplication = createConfidentialClientApplication()
    private var cachedToken: AadToken? = null

    override fun getToken(): AadToken = refreshToken(this.cachedToken)

    private fun refreshToken(aadToken: AadToken?) =
        if (aadToken == null || tokenExpiresWithinTwoMinutes(aadToken)) getAadToken() else aadToken

    private fun tokenExpiresWithinTwoMinutes(aadToken: AadToken) =
        LocalDateTime.now() >= aadToken.expires.minusMinutes(2)

    private fun getAadToken(): AadToken {
        val clientCredentialParameters = ClientCredentialParameters.builder(scopes).build()
        val authenticationResult = confidentialClientApplication.acquireToken(clientCredentialParameters).get()
        LOG.info("Fetching new AadToken")
        return AadToken(
            authenticationResult.accessToken(),
            authenticationResult.expiresOnDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        ).also { cachedToken = it }
    }

    private fun createConfidentialClientApplication() =
        ConfidentialClientApplication.builder(clientId, clientSecret).authority(authorityUrl).build()

    internal data class AadToken(
        val accessToken: String,
        val expires: LocalDateTime
    )

    private companion object EnvironmentKeys {
        private const val WELL_KNOWN_URL = "AZURE_APP_WELL_KNOWN_URL"
        private const val CLIENT_ID = "AZURE_APP_CLIENT_ID"
        private const val CLIENT_PASSWORD = "AZURE_APP_CLIENT_SECRET"
        private const val TARGET_API_ID = "AZURE_APP_TARGET_API_ID"
    }
}