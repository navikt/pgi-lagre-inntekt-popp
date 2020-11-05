package no.nav.pgi.popp.lagreinntekt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.pgi.popp.lagreinntekt.popp.PensjonsgivendeInntektDto

internal fun PensjonsgivendeInntektDto.toJson(): String = ObjectMapper()
        .registerModule(KotlinModule())
        .writeValueAsString(this)