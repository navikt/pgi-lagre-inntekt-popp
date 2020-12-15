package no.nav.pgi.popp.lagreinntekt.mock

import no.nav.pgi.popp.lagreinntekt.popp.PoppClient
import no.nav.pgi.popp.lagreinntekt.popp.token.AadTokenClient.AadToken
import java.time.LocalDateTime
import java.time.Month

internal class TokenProviderMock : PoppClient.TokenProvider {
    override fun getToken(): AadToken {
        return AadToken(
            accessToken = "someToken",
            expires = LocalDateTime.of(2100, Month.DECEMBER, 31, 14, 30)
        )
    }
}