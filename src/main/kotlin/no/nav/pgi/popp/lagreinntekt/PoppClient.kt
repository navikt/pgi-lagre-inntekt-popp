package no.nav.pgi.popp.lagreinntekt

import khttp.post
import khttp.responses.Response
import no.nav.pgi.popp.lagreinntekt.PensjonsgivendeInntektMapper.toJson
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt

class PoppClient(private val url: String) {

    internal fun lagreInntekt(pensjonsgivendeInntekt: PensjonsgivendeInntekt): Response {
        return post(url, data = toJson(pensjonsgivendeInntekt))
    }
}