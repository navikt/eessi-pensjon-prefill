package no.nav.eessi.pensjon.fagmodul.models

data class Landspesifikk (
        var norge: Norge? = null
)

data class Norge (
        var alderspensjon: Alderspensjon? = null,
        var etterlatte: Etterlatte? = null,
        var ufore: Ufore? = null
)

data class Ufore (
        var nasjonaliteter: List<Nasjonalitet>? = null,
        var brukerInfo: BrukerInfo? = null,
        var andreRessurserInntektskilder: AndreRessurserInntektskilder? = null,
        var ytelseInfo: YtelseInfo? = null,
        var ektefelleInfo: EktefelleInfo? = null,
        var barnInfo: List<BarnInfo>? = null,
        var tilgjengeligInfo: String? = null,
        var grunn: String? = null
)

data class Alderspensjon (
        var brukerInfo: BrukerInfo? = null,
        var ansettelsesforhold: Ansettelsesforhold? = null,
        var ytelseInfo: YtelseInfo? = null,
        var ektefelleInfo: EktefelleInfo? = null,
        var barnInfo: List<BarnInfo>? = null,
        var pensjonsgrad: String? = null,
        var tilgjengeligInfo: String? = null,
        var grunn: String? = null
    )

data class Etterlatte (
        var sosken: Sosken? = null,
        var ansettelsesforhold: Ansettelsesforhold? = null,
        var sokersYtelser: YtelseInfo? = null,
        var avdod: Avdod? = null,
        var tilgjengeligInfo: String? = null,
        var grunn: String? = null
)

data class Arbeidsgiver (
        var ansattIdentifikasjonAvArbeidsgiver: AnsattIdentifikasjonAvArbeidsgiver? = null,
        var arbeidsgiverIdentifikasjonAvArbeidsgiver: ArbeidsgiverIdentifikasjonAvArbeidsgiver? = null //arbeidsgiverIdentifikasjon
)

data class AnsattIdentifikasjonAvArbeidsgiver (
        var ansattIndikator: String? = null,
        var navn: String? = null,
        var adresse: Adresse? = null,
        var arbeidsgiverIndikator: String? = null
)

data class ArbeidsgiverIdentifikasjonAvArbeidsgiver (
        var arbeidsgiverIndikator: String? = null,
        var registreringsNummer: String? = null,
        var personNummer: String? = null,
        var skatteNummer: String? = null,
        var bedriftsRegister: String? = null
)

data class Avdod (
        var nasjonaliteter: List<Nasjonalitet>? = null,
        var pensjonsMottaker: PensjonsMottaker? = null,
        var inntektsgivendeArbeid: String? = null
)

data class Sosken (
        var nasjonaliteter: List<Nasjonalitet>? = null,
        var forsorgelsesplikt: String? = null,
        var arbeidsforhet: String? = null,
        var arbeidsufor: String? = null,
        var soskenNavn: List<SoskenNavn>? = null
)

data class SoskenNavn (
        var navn: String? = null,
        var personnummer: String? = null,
        var borMedSosken: String? = null
)

data class BrukerInfo (
        var borMedEktefelleEllerPartner: String? = null,
        var boddFrahverandreSiden: String? = null,
        var samboer: Samboer? = null,
        var arbeidsAvklaringskurs: String? = null,
        var yrke: String? = null
)

data class Ansettelsesforhold (
        var ansettelsesforholdType: String? = null,
        var lonnsInntekt: List<Belop>? = null,
        var andreInntektskilder: AndreInntektskilder? = null,
        var ingenInntektOppgitt: String? = null,
        var obligatoriskPensjonsDekning: String? = null,
        var inntektsType: String? = null
)

data class YtelseInfo (
        var kontantYtelserSykdom: String? = null,
        var hjelpestonad: String? = null,
        var grunnleggendeYtelser: String? = null,
        var ytelserTilUtdanning: String? = null,
        var forsorgelseBarn: String? = null,
        var frivilligInnskudd: String? = null
)

data class EktefelleInfo (
        var familiestatus: Familiestatus? = null,
        var nasjonalitet: List<Nasjonalitet>? = null,
        var inntektsgivedeArbeid: InntektsgivendeArbeid? = null,
        var arbeidsfor: String? = null,
        var pensjonsmottaker: List<PensjonsMottaker>?  = null,
        var ikkeYrkesaktiv: String? = null,
        var andreYtelser: AndreYtelser? = null,
        var andreRessurserInntektskilder: AndreRessurserInntektskilder? = null
)

data class BarnInfo (
        var etternavn: String? = null,
        var fornavn: String? = null,
        var familiestatus: List<Familiestatus>? = null,
        var barnetrygd: Barnetrygd? = null,
        var adresse: Adresse? = null,
        var borMedBeggeForeldre: String? = null,
        var forsikredeForsorgerBarnet: String? = null,
        var barnetBorHosForsikrede: String? = null
)

data class Barnetrygd (
        var barnetrygd: String? = null,
        var typeBarnetrygd: String? = null,
        var belop: List<Belop>? = null
)

data class AndreRessurserInntektskilder (
        var andreRessurserInntektskilder: String? = null,
        var typeAndreRessurserInntektskilder: String? = null,
        var belop: List<Belop>? = null,
        var oppgirIngenInntekt: String? = null,
        var arbeidsgiver: List<Arbeidsgiver>? = null,
        var startDatoAnsettelse: String? = null
)

data class AndreYtelser (
        var andreYtelser: String? = null,
        var andreYtelserType: String? = null,
        var belop: List<Belop>? = null
)

//4.4.5 Pensjonsmottaker
data class PensjonsMottaker (
        var erPensjonsmottaker: String? = null,
        var typePensjon: String? = null,
        var pensjonsnummer: String? = null,
        var institusjonsopphold: Institusjonsopphold? = null,
        var startDatoYtelse: String? = null,
        var sluttDatoYtelse: String? = null,
        var pensjonBasertPa: String? = null
)

data class Institusjonsopphold (
        var land: String? = null,
        var personNummer: String? = null,
        var sektor: String? = null,
        var institusjon: Institusjon? = null,
        var belop: List<Belop>? = null
)

data class InntektsgivendeArbeid (
        var inntektsgivendeArbeid: String? = null,
        var belop: List<Belop>? = null
)

data class Familiestatus (
        var familiestatus: String? = null,
        var datoFamilieStatus: String? = null
)

data class AndreInntektskilder (
        var andreInntektskilderIndikator: String? = null,
        var typeAndreInntektskilder: String? = null,
        var andreInntektskilderBelop: List<Belop>? = null
)

data class Belop (
        var belop: String? = null,
        var valuta: String? = null,
        var belopGjelderFra: String? = null,
        var betalingshyppighet: String? = null,
        var annenBetalingshyppighet: String? = null
)

data class Samboer (
        var boddSammenSiden: String? = null,
        var barnMedSamboer: String? = null,
        var tidligereGiftMedSamboer: String? = null,
        var nasjonaliteter: List<Nasjonalitet>? = null,
        var forsikringMotArbeidsuforhet: String? = null
)

data class Nasjonalitet (
        var nasjonalitet: String? = null
)