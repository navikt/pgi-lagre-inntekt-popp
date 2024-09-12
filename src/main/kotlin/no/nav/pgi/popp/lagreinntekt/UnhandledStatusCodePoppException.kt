package no.nav.pgi.popp.lagreinntekt

import no.nav.pgi.popp.lagreinntekt.util.maskFnr
import java.net.http.HttpResponse

internal class UnhandledStatusCodePoppException(response: HttpResponse<String>) :
    Exception("""Unhandled status code in PoppResponse(Status: ${response.statusCode()} Body: ${response.body()})""".maskFnr())