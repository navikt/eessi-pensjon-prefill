package no.nav.eessi.eessifagmodul.models

import com.fasterxml.jackson.annotation.JsonProperty


/**
 *  Pensjon objekt er her!
 *
 */
data class Pensjon(
        var reduksjon: List<ReduksjonItem>? = null,
        var vedtak: List<VedtakItem>? = null,
        var sak: Sak? = null,

        var gjenlevende: Bruker? = null,
        var bruker: Bruker? = null,

        var tilleggsinformasjon: Tilleggsinformasjon? = null,
        //p2000 - p2200
        val ytterligeinformasjon: String? = null,
        val etterspurtedokumenter: String? = null,
        val ytelser: List<YtelserItem>? = null,
        val forespurtstartdato: String? = null,
        //P5000
        var medlemskapAnnen: List<MedlemskapItem>? = null,
        var medlemskapTotal: List<MedlemskapItem>? = null,
        var medlemskap: List<MedlemskapItem>? = null,
        var trygdetid: List<MedlemskapItem>? = null,

        var institusjonennaaikkesoektompensjon: List<String>? = null,

        var utsettelse: List<Utsettelse>? = null,
        //P2000, P2100, P2200
        val vedlegg: List<String>? = null,

        val vedleggandre: String? = null,
        val angitidligstdato: String? = null

)

//P2000
data class Utsettelse(
        val institusjon: Institusjon? = null,
        val tildato : String? = null
)

//P5000
data class MedlemskapItem(
        val relevans: String? = null,
        val ordning: String? = null,
        val land: String? = null,
        val sum: TotalSum? = null,
        val yrke: String? = null,
        val gyldigperiode: String? = null,
        val type: String? = null,
        val beregning: String? = null,
        val periode: Periode? = null
)
//P5000
data class Dager(
        val nr: String? = null,
        val type: String? = null
)


//P5000
data class TotalSum(
        val kvartal: String? = null,
        val aar: String? = null,
        val uker: String? = null,
        val dager: Dager? = null,
        val maaneder: String? = null
)

//P2000 - P2200
data class YtelserItem(
        val annenytelse: String? = null,
        val totalbruttobeloeparbeidsbasert: String? = null,
        val institusjon: Institusjon? = null,
        val pin: PinItem? = null,
        val startdatoutbetaling: String? = null,
        val mottasbasertpaa: String? = null,
        val ytelse: String? = null,
        val totalbruttobeloepbostedsbasert: String? = null,
        val startdatoretttilytelse: String? = null,
        val beloep: List<BeloepItem>? = null,
        val sluttdatoretttilytelse: String? = null,
        val sluttdatoutbetaling: String? = null,
        val status: String? = null
)

//data class YtelserItem2(
//
//        val annenytelse: String? = null,
//        val institusjon: Institusjon? = null,
//
//        val pin: PinItem? = null,
//        val startdatoutbetaling: String? = null,
//
//        val ytelse: String? = null,
//
//        val startdatoretttilytelse: String? = null,
//
//        val beloep: List<BeloepItem?>? = null,
//        val sluttdatoutbetaling: String? = null,
//        val status: String? = null
//)

data class BeloepItem(
       val annenbetalingshyppighetytelse: String? = null,
       val betalingshyppighetytelse: String? = null,
       val valuta: String? = null,
       val beloep: String? = null,
       val gjeldendesiden: String? = null
)

data class Sak(
        val artikkel54: String? = null,
        @JsonProperty("reduksjon")
        val reduksjon: List<ReduksjonItem>? = null,
        @JsonProperty("kravtype")
        val kravtype: List<KravtypeItem>? = null,
        val enkeltkrav: KravtypeItem? = null

)

data class KravtypeItem(
        @JsonProperty("datoFrist")
        val datoFrist: String? = null,
        val krav: String? = null
)

data class VedtakItem(
        val trekkgrunnlag : List<String>? = null,
        val mottaker : List<String>? = null,
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
        val virkningsdato: List<VirkningsdatoItem>? = null,
        val aarsak: Arsak? = null,
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
        val tom: String? = null,
        val extra: String? = null
)

/***
 *
 * end of data class. Pensjon/P6000,P2000,P4000,P5000
 *
 */

