package no.nav.eessi.eessifagmodul.models


data class PersonDetail(
        var sakType: String? = null,
        var buc: String? = null,
        var personNavn: String? = null,
        var fnr: String? = null,
        var aktoerId: String? = null,
        var euxCaseId: String? = null
)

data class Pensjon(
        var reduksjon: List<ReduksjonItem>? = null,
        var vedtak: List<VedtakItem>? = null,
        var sak: Sak? = null,

        var gjenlevende: Bruker? = null,
        var bruker: Bruker? = null,

        var tilleggsinformasjon: Tilleggsinformasjon? = null,
        //p2000 - p2200
        var ytterligeinformasjon: String? = null,
        var etterspurtedokumenter: String? = null,
        var ytelser: List<YtelserItem>? = null,
        var forespurtstartdato: String? = null,
        //P5000
        var medlemskapAnnen: List<MedlemskapItem>? = null,
        var medlemskapTotal: List<MedlemskapItem>? = null,
        var medlemskap: List<MedlemskapItem>? = null,
        var trygdetid: List<MedlemskapItem>? = null,

        var institusjonennaaikkesoektompensjon: List<String>? = null,

        var utsettelse: List<Utsettelse>? = null,
        //P2000, P2100, P2200
        var vedlegg: List<String>? = null,

        var vedleggandre: String? = null,
        var angitidligstdato: String? = null,
        var antallSokereKjent: String? = null //P2100 11.7
)

//P2000
data class Utsettelse(
        var institusjon: Institusjon? = null,
        var tildato: String? = null
)

//P5000
data class MedlemskapItem(
        var relevans: String? = null,
        var ordning: String? = null,
        var land: String? = null,
        var sum: TotalSum? = null,
        var yrke: String? = null,
        var gyldigperiode: String? = null,
        var type: String? = null,
        var beregning: String? = null,
        var periode: Periode? = null
)

//P5000
data class Dager(
        var nr: String? = null,
        var type: String? = null
)


//P5000
data class TotalSum(
        var kvartal: String? = null,
        var aar: String? = null,
        var uker: String? = null,
        var dager: Dager? = null,
        var maaneder: String? = null
)

//P2000 - P2200
data class YtelserItem(
        var annenytelse: String? = null,
        var totalbruttobeloeparbeidsbasert: String? = null,
        var institusjon: Institusjon? = null,
        var pin: PinItem? = null,
        var startdatoutbetaling: String? = null,
        var mottasbasertpaa: String? = null,
        var ytelse: String? = null,
        var totalbruttobeloepbostedsbasert: String? = null,
        var startdatoretttilytelse: String? = null,
        var beloep: List<BeloepItem>? = null,
        var sluttdatoretttilytelse: String? = null,
        var sluttdatoutbetaling: String? = null,
        var status: String? = null,
        var ytelseVedSykdom: String? = null //7.2 //P2100
)

data class BeloepItem(
        var annenbetalingshyppighetytelse: String? = null,
        var betalingshyppighetytelse: String? = null,
        var valuta: String? = null,
        var beloep: String? = null,
        var gjeldendesiden: String? = null
)

data class Sak(
        var artikkel54: String? = null,
        var reduksjon: List<ReduksjonItem>? = null,
        var kravtype: List<KravtypeItem>? = null,
        var enkeltkrav: KravtypeItem? = null

)

data class KravtypeItem(
        var datoFrist: String? = null,
        var krav: String? = null
)

data class VedtakItem(
        var trekkgrunnlag: List<String>? = null,
        var mottaker: List<String>? = null,
        var grunnlag: Grunnlag? = null,
        var begrunnelseAnnen: String? = null,
        var artikkel: String? = null,
        var virkningsdato: String? = null,
        var ukjent: Ukjent? = null,
        var type: String? = null,
        var resultat: String? = null,
        var beregning: List<BeregningItem>? = null,
        var avslagbegrunnelse: List<AvslagbegrunnelseItem>? = null,
        var kjoeringsdato: String? = null,
        var basertPaa: String? = null,
        var basertPaaAnnen: String? = null,
        var delvisstans: Delvisstans? = null
)

data class Tilleggsinformasjon(
        var annen: Annen? = null,
        var anneninformation: String? = null,
        var saksnummer: String? = null,
        var person: Person? = null,
        var dato: String? = null,
        var andreinstitusjoner: List<AndreinstitusjonerItem>? = null,
        var saksnummerAnnen: String? = null,
        var artikkel48: String? = null,
        var opphoer: Opphoer? = null
)

//TODO: endre til bruk av Institusjon og Institusjonsadresse
data class AndreinstitusjonerItem(
        var institusjonsid: String? = null,
        var institusjonsnavn: String? = null,
        var institusjonsadresse: String? = null,
        var postnummer: String? = null,
        var bygningsnr: String? = null,
        var land: String? = null,
        var region: String? = null,
        var poststed: String? = null
)

data class Annen(
        var institusjonsadresse: Institusjonsadresse? = null
)

data class Delvisstans(
        var utbetaling: Utbetaling? = null,
        var indikator: String? = null
)

data class Ukjent(
        var beloepBrutto: BeloepBrutto? = null
)

data class ReduksjonItem(
        var type: String? = null,
        var virkningsdato: List<VirkningsdatoItem>? = null,
        var aarsak: Arsak? = null,
        var artikkeltype: String? = null
)


//TODO: endre til Periode?
data class VirkningsdatoItem(
        var startdato: String? = null,
        var sluttdato: String? = null
)

data class Arsak(
        var inntektAnnen: String? = null,
        var annenytelseellerinntekt: String? = null
)

data class Opphoer(
        var dato: String? = null,
        var annulleringdato: String? = null
)

data class Utbetaling(
        var begrunnelse: String? = null,
        var valuta: String? = null,
        var beloepBrutto: String? = null
)

data class Grunnlag(
        var medlemskap: String? = null,
        var opptjening: Opptjening? = null,
        var framtidigtrygdetid: String? = null
)

data class Opptjening(
        var forsikredeAnnen: String? = null
)

data class AvslagbegrunnelseItem(
        var begrunnelse: String? = null,
        var annenbegrunnelse: String? = null
)

data class BeregningItem(
        var beloepNetto: BeloepNetto? = null,
        var valuta: String? = null,
        var beloepBrutto: BeloepBrutto? = null,
        var utbetalingshyppighetAnnen: String? = null,
        var periode: Periode? = null,
        var utbetalingshyppighet: String? = null
)

data class BeloepNetto(
        var beloep: String? = null
)

data class BeloepBrutto(
        var ytelseskomponentTilleggspensjon: String? = null,
        var beloep: String? = null,
        var ytelseskomponentGrunnpensjon: String? = null,
        var ytelseskomponentAnnen: String? = null
)

data class Periode(
        var fom: String? = null,
        var tom: String? = null,
        var extra: String? = null
)
