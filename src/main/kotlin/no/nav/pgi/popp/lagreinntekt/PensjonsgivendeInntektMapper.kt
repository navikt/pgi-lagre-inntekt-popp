package no.nav.pgi.popp.lagreinntekt

import com.fasterxml.jackson.databind.ObjectMapper
import io.confluent.kafka.schemaregistry.client.rest.Versions.JSON
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt



internal fun PensjonsgivendeInntekt.toJson(): String =
        ObjectMapper().writeValueAsString(this.toString())

