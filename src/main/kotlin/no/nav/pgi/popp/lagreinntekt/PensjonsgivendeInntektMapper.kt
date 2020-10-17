package no.nav.pgi.popp.lagreinntekt

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt

internal object PensjonsgivendeInntektMapper {
    private val objectMapper = ObjectMapper()

    internal fun toJson(pensjonsgivendeInntekt: PensjonsgivendeInntekt): String =
            objectMapper.writeValueAsString(pensjonsgivendeInntekt.toString())
}