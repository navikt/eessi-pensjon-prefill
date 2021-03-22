package no.nav.eessi.pensjon.fagmodul.sedmodel

import com.fasterxml.jackson.annotation.JsonIgnoreProperties


class P5000Pensjon(
    val trygdetid: List<MedlemskapItem>? = null,
    // Benyttes for visning av "Se annen" siden for P5000 i fontend
    val medlemskapAnnen: List<MedlemskapItem>? = null,
    // Benyttes for visning av "Se oversikt" siden for 5000 i frontend
    val medlemskap: List<MedlemskapItem>? = null
)

/**
 * Benyttes både ved prefill og ved visning av trygdetidsperioder til frontend
 *
 * Flere felter her er ubrukt i javakoden men benyttes av frontend
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedlemskapItem(
    val relevans: String? = null,
    val ordning: String? = null,
    val land: String? = null,
    val sum: TotalSum? = null,
    val yrke: String? = null,
    val gyldigperiode: String? = null,
    val type: String? = null,
    val beregning: String? = null,
    val informasjonskalkulering: String? = null,
    val periode: Periode? = null,
    val enkeltkrav: KravtypeItem? = null
)

data class Dager(
    val nr: String? = null,
    val type: String? = null
)

data class TotalSum(
    val kvartal: String? = null,
    val aar: String? = null,
    val uker: String? = null,
    val dager: Dager? = null,
    val maaneder: String? = null
)