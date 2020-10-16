package no.nav.pgi.popp.lagreinntekt

import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import khttp.post
import khttp.responses.Response
import org.json.JSONObject
import java.net.URL

class PoppClient(private val url: String) {

    internal fun lagreInntekt(pensjonsgivendeInntekt: PensjonsgivendeInntekt) : Response = post(url, json = pensjonsgivendeInntekt)
}