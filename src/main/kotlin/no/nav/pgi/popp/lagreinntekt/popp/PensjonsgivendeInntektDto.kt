package no.nav.pgi.popp.lagreinntekt.popp


import no.nav.samordning.pgi.schema.HendelseKey
import no.nav.samordning.pgi.schema.PensjonsgivendeInntekt
import no.nav.samordning.pgi.schema.PensjonsgivendeInntektPerOrdning
import org.apache.kafka.clients.consumer.ConsumerRecord


internal fun mapToPensjonsgivendeInntektDto(consumerRecord: ConsumerRecord<HendelseKey, PensjonsgivendeInntekt>): PensjonsgivendeInntektDto =
        consumerRecord.value().mapToPensjonsgivendeInntektDto()


internal fun PensjonsgivendeInntekt.mapToPensjonsgivendeInntektDto(): PensjonsgivendeInntektDto =
        PensjonsgivendeInntektDto(
                norskPersonidentifikator = getNorskPersonidentifikator(),
                inntektsaar = getInntektsaar(),
                pensjonsgivendeInntekt = getPensjonsgivendeInntekt()
                        .map { it.mapToPensjonsgivendeInntektPerOrdningDto() }
                        .toList()
        )


private fun PensjonsgivendeInntektPerOrdning.mapToPensjonsgivendeInntektPerOrdningDto(): PensjonsgivendeInntektPerOrdningDto {
    return PensjonsgivendeInntektPerOrdningDto(
            skatteordning = getSkatteordning().name,
            datoForFastetting = getDatoForFastetting(),
            pensjonsgivendeInntektAvLoennsinntekt = getPensjonsgivendeInntektAvLoennsinntekt(),
            pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel = getPensjonsgivendeInntektAvLoennsinntektBarePensjonsdel(),
            pensjonsgivendeInntektAvNaeringsinntekt = getPensjonsgivendeInntektAvNaeringsinntekt(),
            pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage = getPensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage()
    )
}

internal data class PensjonsgivendeInntektDto(
        val norskPersonidentifikator: String?,
        val inntektsaar: String,
        val pensjonsgivendeInntekt: List<PensjonsgivendeInntektPerOrdningDto> = emptyList()
)

internal data class PensjonsgivendeInntektPerOrdningDto(
        val skatteordning: String?,
        val datoForFastetting: String?,
        val pensjonsgivendeInntektAvLoennsinntekt: Long?,
        val pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel: Long?,
        val pensjonsgivendeInntektAvNaeringsinntekt: Long?,
        val pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage: Long?
)