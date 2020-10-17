package no.nav.pgi.popp.lagreinntekt

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import khttp.post
import khttp.responses.Response
import no.nav.pgi.popp.lagreinntekt.PensjonsgivendeInntektMapper.toJson

class PoppClient(private val url: String) {

    internal fun lagreInntekt(pensjonsgivendeInntekt: PensjonsgivendeInntekt) : Response {
        return post(url, json = toJson(pensjonsgivendeInntekt))
    }
}