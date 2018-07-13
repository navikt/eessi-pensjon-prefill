package no.nav.eessi.eessifagmodul.models

import com.fasterxml.jackson.annotation.JsonProperty



/**
 *  Pensjon objekt er her!
 *
 */
data class Pensjon(
        @JsonProperty("reduksjon")
        var reduksjon: List<ReduksjonItem>? = null,
        @JsonProperty("vedtak")
        var vedtak: List<VedtakItem?>? = null,
        var sak: Sak? = null,
        @JsonProperty("gjenlevende")
        var gjenlevende: Bruker? = null,
        //var gjenlevende: Gjenlevende? = null,
        @JsonProperty("tilleggsinformasjon")
        var tilleggsinformasjon: Tilleggsinformasjon? = null
)

data class Sak(
        val artikkel54: String? = null,
        @JsonProperty("reduksjon")
        val reduksjon: List<ReduksjonItem>? = null,
        @JsonProperty("kravtype")
        val kravtype: List<KravtypeItem>? = null
)

data class KravtypeItem(
        @JsonProperty("datoFrist")
        val datoFrist: String? = null
)

data class VedtakItem(
        val grunnlag: Grunnlag? = null,
        val begrunnelseAnnen: String? = null,
        val artikkel: String? = null,
        val virkningsdato: String? = null,
        val ukjent: Ukjent? = null,
        val type: String? = null,
        val resultat: String? = null,
        @JsonProperty("beregning")
        val beregning: List<BeregningItem>? = null,
        @JsonProperty("avslagbegrunnelse")
        val avslagbegrunnelse: List<AvslagbegrunnelseItem>? = null,
        val kjoeringsdato: String? = null,
        val basertPaa: String? = null,
        val basertPaaAnnen: String? = null,
        val delvisstans: Delvisstans? = null
)

data class Tilleggsinformasjon(
        val annen: Annen? = null,
        val anneninformation: String? = null,
        val saksnummer: String? = null,
        val person: Person? = null,
        val dato: String? = null,
        @JsonProperty("andreinstitusjoner")
        val andreinstitusjoner: List<AndreinstitusjonerItem>? = null,
        val saksnummerAnnen: String? = null,
        val artikkel48: String? = null,
        val opphoer: Opphoer? = null
)

data class AndreinstitusjonerItem(
        val institusjonsadresse: String? = null,
        val postnummer: String? = null,
        val bygningsnr: String? = null,
        val land: String? = null,
        val region: String? = null,
        val poststed: String? = null
)

data class Annen(
        val institusjonsadresse: Institusjonsadresse? = null
)

data class Delvisstans(
        val utbetaling: Utbetaling? = null,
        val indikator: String? = null
)

data class Ukjent(
        val beloepBrutto: BeloepBrutto? = null
)

data class ReduksjonItem (
        val type: String? = null,
        @JsonProperty("virkningsdato")
        val virkningsdato: List<VirkningsdatoItem>? = null,
        val arsak: Arsak? = null,
        val artikkeltype: String? = null
)

data class VirkningsdatoItem(
       val startdato: String? = null,
       val sluttdato: String? = null
)

data class Arsak(
        val inntektAnnen: String? = null,
        val annenytelseellerinntekt: String? = null
)

data class Opphoer(
       val dato: String? = null,
       val annulleringdato: String? = null
)

data class Utbetaling(
        val begrunnelse: String? = null,
        val valuta: String? = null,
        val beloepBrutto: String? = null
)

data class Grunnlag(
        val medlemskap: String? = null,
        val opptjening: Opptjening? = null,
        val framtidigtrygdetid: String? = null
)

data class Opptjening(
        val forsikredeAnnen: String? = null
)

data class AvslagbegrunnelseItem(
       val begrunnelse: String? = null,
       val annenbegrunnelse: String? = null
)

data class BeregningItem(
       val beloepNetto: BeloepNetto? = null,
        val valuta: String? = null,
        val beloepBrutto: BeloepBrutto? = null,
        val utbetalingshyppighetAnnen: String? = null,
        val periode: Periode? = null,
        val utbetalingshyppighet: String? = null
)

data class BeloepNetto(
        val beloep: String? = null
)

data class BeloepBrutto(
       val ytelseskomponentTilleggspensjon: String? = null,
       val beloep: String? = null,
       val ytelseskomponentGrunnpensjon: String? = null,
       val ytelseskomponentAnnen: String? = null
)

data class Periode(
        val fom: String? = null,
        val tom: String? = null
)

/***
 *
 * end of data class. Pensjon/P6000
 *
 */

