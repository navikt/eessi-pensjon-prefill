package no.nav.eessi.pensjon.fagmodul.sedmodel

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

class MeldingOmPensjon(
		val melding: String?,
		val pensjon: Pensjon
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Pensjon(
		val gjenlevende: Bruker? = null, // Brukes fleres steder

		//P2000
		val angitidligstdato: String? = null,

		//P2XXX
		val ytelser: List<YtelserItem>? = null,
		val forespurtstartdato: String? = null,
		val kravDato: Krav? = null, //kravDato pkt. 9.1 P2000

	//P3000
		val landspesifikk: Landspesifikk? = null,


		//MOCK Pesys
		val medlemskapAnnen: List<MedlemskapItem>? = null,
		val medlemskapTotal: List<MedlemskapItem>? = null,
		val medlemskap: List<MedlemskapItem>? = null,

		//P5000
		val trygdetid: List<MedlemskapItem>? = null,
		val institusjonennaaikkesoektompensjon: List<String>? = null,

        //P8000
		val anmodning: AnmodningOmTilleggsInfo? = null,

        //P7000
		val samletVedtak: SamletMeldingVedtak? = null,
		val bruker: Bruker? = null
		)

//P10000
data class Ytelsesdatoer(
		val annenytelse: String? = null,
		val datotyper: Datotyper? = null,
		val personkravstatus: String? = null,
		val beloep: List<BeloepItem>? = null
)

//P10000
data class Datotyper (
		val startdatoutbetaling: String? = null,
		val sluttdatoutbetaling: String? = null,

		val startdatoforstansytelse: String? = null,
		val sluttdatorettytelse: String? = null,

		val startdatoretttilytelser: String? = null,
		val sluttdatoredusertytelse: String? = null,

		val startdatoredusertytelse: String? = null,
		val sluttdatoforstansiytelser: String? = null,

		val datoavslagpaaytelse: String? = null,
		val datokravytelse: String? = null
)

//P7000
data class SamletMeldingVedtak(
		val avslag: List<PensjonAvslagItem>? = null,
		val vedtaksammendrag: String? = null,
		val tildeltepensjoner: TildeltePensjoner? = null,
		val startdatoPensjonsRettighet: String? = null,  // 4.1.5
		val reduksjonsGrunn: String? = null    // 4.1.7
)

//P7000-5
data class PensjonAvslagItem(
        val pensjonType: String?= null,
        val begrunnelse: String? = null, //5.1
        val dato: String? = null,   //5.2
        val datoFrist: String? = null,
        val pin : PinItem? = null,
        val adresse: String? = null
)

//Institusjon
data class Institusjon(
        val institusjonsid: String? = null,
        val institusjonsnavn: String? = null,
        val saksnummer: String? = null,
        val sektor: String? = null,
        val land: String? = null,
        val pin: String? = null,
        val personNr: String? = null,
        val innvilgetPensjon: String? = null,  // 4.1.3.
        val utstedelsesDato: String? = null,  //4.1.4.
        val startdatoPensjonsRettighet: String? = null  //4.1.5
)

//P8000
data class AnmodningOmTilleggsInfo(
		val relasjonTilForsikretPerson: String? =  null, //4.1.1
		val beskrivelseAnnenSlektning: String? = null, // 4.2.1
		val referanseTilPerson: String? = null,
		val anmodningPerson: AnmodningOmPerson? = null, //10.
		val anmodningOmBekreftelse: AnmodningOmBekreftelse? = null, //9.
		val informasjon: AnmodningOmInformasjon? = null, //8
		val ytterligereInfoOmDokumenter: String? = null, //6.1
		val begrunnKrav: String? = null,
		val seder: List<SedAnmodningItem>? = null,
		val personAktivitet: List<PersonAktivitetItem>? = null,
		val personAktivitetSom: List<PersonAktivitetSomItem>? = null,
		val personInntekt: List<PersonensInntekt>? = null,
		val annenInfoOmYtelse: String? = null    //8.5
)

// 8.4
data class PersonensInntekt(
        val oppgiInntektFOM: String? = null,
        val personInntekt: List<PersonInntektItem>? = null
)

data class AnmodningOmInformasjon(
		val generellInformasjon: List<GenerellInfo>? = null, // 8.1
		val infoOmPersonYtelse: List<InfoOmPersonYtelse>? = null, // 8.2
		val annenEtterspurtInformasjon: String? = null,
		val begrunnelseKrav: String? = null //8.6
)

// Alt i denne blokken er 8.2
data class InfoOmPersonYtelse(
        val informerOmPersonFremsattKravEllerIkkeEllerMottattYtelse: String? = null, // 8.2.1
        val annenYtelse: String? = null, //8.2.2.1
        val sendInfoOm: List<SendInfoOm>? = null // 8.2.3.1
)

data class SendInfoOm(
        val sendInfoOm: String? = null,
        val annenInfoOmYtelser: String? = null // 8.2.3.2.1
)

data class AnmodningOmPerson(
        val egenerklaering: String? = null, //10.1
        val begrunnelseKrav: String? = null //10.2
)

data class AnmodningOmBekreftelse(
        val bekreftelseInfo: String? = null,
        val bekreftelsesGrunn: String? = null //9.2
)

data class GenerellInfo(
        val generellInfoOmPers: String? = null
)

data class SedAnmodningItem(
        val begrunnelse: String? = null,
        val andreEtterspurteSEDer: String? = null,
        val sendFolgendeSEDer: List<String>? = null //7.1.1
)

data class PersonAktivitetItem(
        val persAktivitet: String? = null
)

data class PersonAktivitetSomItem(
        val persAktivitetSom: String? = null
)

data class PersonInntektItem(
        val persInntekt: String? = null
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
		val informasjonskalkulering: String? = null,
		val periode: Periode? = null,
		val enkeltkrav: KravtypeItem? = null

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

//P2000 - P2200 - //P10000
data class YtelserItem(
		val annenytelse: String? = null,
		val totalbruttobeloeparbeidsbasert: String? = null,
		val institusjon: Institusjon? = null,
		val pin: PinItem? = null,
		val startdatoutbetaling: String? = null,
		val mottasbasertpaa: String? = null,
		val mottasbasertpaaitem: List<MottasBasertPaaItem>? = null,
		val ytelse: String? = null,
		val totalbruttobeloepbostedsbasert: String? = null,
		val startdatoretttilytelse: String? = null,
		val beloep: List<BeloepItem>? = null,
		val sluttdatoretttilytelse: String? = null,
		val sluttdatoutbetaling: String? = null,
		val status: String? = null,
		val ytelseVedSykdom: String? = null, //7.2 //P2100

        //P10000
		val annenbetalingshyppighet: String? = null,
		val datotyper: Datotyper? = null,
		val anneninfoomytelsertekst: String? = null,
		val tilleggsytelserutbetalingitilleggtilpensjon: String? = null,
		val ytelsesdatoer: Ytelsesdatoer? = null,
		val ytelsestype: String? = null

)

data class MottasBasertPaaItem(
		val verdi: String? = null,
		val totalbruttobeloepbostedsbasert: String? = null,
		val totalbruttobeloeparbeidsbasert: String? = null
)

data class BeloepItem(
        val annenbetalingshyppighetytelse: String? = null,
        val betalingshyppighetytelse: String? = null,
        val valuta: String? = null,
        val beloep: String? = null,
        val gjeldendesiden: String? = null
)

data class Sak(
		val artikkel54: String? = null,
		val reduksjon: List<ReduksjonItem>? = null,
		val kravtype: List<KravtypeItem>? = null,
		val enkeltkrav: KravtypeItem? = null
)

data class KravtypeItem(
        val datoFrist: String? = null,
        val krav: String? = null
)

data class VedtakItem(
		val trekkgrunnlag: List<String>? = null,
		val mottaker: List<String>? = null,
		val grunnlag: Grunnlag? = null,
		val begrunnelseAnnen: String? = null,
		val artikkel: String? = null,
		val virkningsdato: String? = null,
		val ukjent: Ukjent? = null,
		val type: String? = null,
		val resultat: String? = null,
		val beregning: List<BeregningItem>? = null,
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
		val andreinstitusjoner: List<AndreinstitusjonerItem>? = null,
		val saksnummerAnnen: String? = null,
		val artikkel48: String? = null,
		val opphoer: Opphoer? = null,
		val tilleggsopplysning: String? = null,

		//P9000
		val p8000: RefP8000? = null,
		val bekreftelseSed: List<BekreftelseSedItem>? = null,

		val ikkeyrkesaktiv: String? = null,
		val arbeidsledig: String? = null,
		val negativtsvar: Negativtsvar? = null,

		val vedlegginfo: String? = null,
		val vedlegg: List<VedleggItem>? = null,

		val yrkesaktivitet: Yrkesaktivitet? = null
)

data class VedleggItem(
		val dokument: String? = null
)

data class BekreftelseSedItem(
		val aarsak: String? = null,
		val p8000ref: String? = null,
		val grunn: String? = null,
		//verder? 01,02 ??
		val info: String? = null
)

//P9000
data class RefP8000(
		//verdier? 01, 02, 03???
		val henvisningperson: String? = null,
		val dato: String? = null
)

//P9000
data class Negativtsvar(
		val aarsakgrunn: String? = null,
		val aarsakannen: String? = null,
		val aarsakikkesendsed: String? = null,
		val dokument: String? = null,
		val informasjon: String? = null,
		val bekreftelseinformasjon: String? = null,
		val sed: String? = null
)

data class Yrkesaktivitet(
		val ingenaktivtetinformasjon: String? = null,
		val tilleggsopplysning: String? = null
)

data class AndreinstitusjonerItem(
        val institusjonsid: String? = null,
        val institusjonsnavn: String? = null,
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

data class ReduksjonItem(
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

//P7000 4. Tildelte pensjoner
data class TildeltePensjoner(
		val pensjonType: String? = null, //4.1.2
		val vedtakPensjonType: String? = null, //4.1.1
		val tildeltePensjonerLand: String? = null,   //4.1.2.1.1.
		val addressatForRevurdering: String? = null,   //4.1.8.2.1.
		val institusjonPensjon: PensjonsInstitusjon? = null,
		val institusjon: Institusjon? = null
)

data class PensjonsInstitusjon(
        val sektor: String? = null
)

data class Institusjonsadresse(
		val poststed: String? = null,
		val postnummer: String? = null,
		val land: String? = null
)
