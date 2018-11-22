package no.nav.eessi.eessifagmodul.models

data class Landspesifikk (
        var norge: Norge? = null
)

data class Norge (
        var alderspensjon: Alderspensjon? = null,
        var etterlatte: Etterlatte,
        var ufore: Ufore? = null
)


//4. Ytterligere informasjon om krav om alderspensjon
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

//5. Ytterligere informasjon om krav om etterlattepensjon
data class Etterlatte (
        var sosken: Sosken? = null,
        var ansettelsesforhold: Ansettelsesforhold? = null,
        var sokersYtelser: YtelseInfo? = null,
        var avdod: Avdod? = null,
        var tilgjengeligInfo: String? = null,
        var grunn: String? = null
)

//5.4, 5.5 & 5.6
data class Avdod (
        var nasjonaliteter: List<Nasjonalitet>? = null,
        var pensjonsMottaker: PensjonsMottaker? = null,
        var inntektsgivendeArbeid: String? = null
)

//5.1 Ytterligere informasjon om søsken
data class Sosken (
        var nasjonaliteter: List<Nasjonalitet>? = null,
        var forsorgelsesplikt: String? = null,
        var arbeidsforhet: String? = null,
        var arbeidsufor: String? = null,
        var soskenNavn: List<SoskenNavn>? = null
)

//5.1.5 Navn på søsken (helsøsken desom søkeren er avdødes eget barn)
data class SoskenNavn (
        var navn: String? = null,
        var personnummer: String? = null,
        var borMedSosken: String? = null
)

//4.1 Ytterligere informasjon om forsikrede
data class BrukerInfo (
        var borMedEktefelleEllerPartner: String? = null,
        var boddFrahverandreSiden: String? = null,
        var samboer: Samboer? = null
)

//4.2 og 5.2 Ytterligere informasjon om forsikredes ansettelsesforhold eller selvstendig næringsvirksomhet
data class Ansettelsesforhold (
        var ansettelsesforholdType: String? = null,
        var lonnsInntekt: List<Belop>? = null,
        var andreInntektskilder: AndreInntektskilder? = null,
        var ingenInntektOppgitt: String? = null,
        var obligatoriskPensjonsDekning: String? = null,
        var inntektsType: String? = null
)

//4.3 Ytterligere informasjon om ytelser den forsikrede mottar
data class YtelseInfo (
        var kontantYtelserSykdom: String? = null,
        var hjelpestonad: String? = null,
        var grunnleggendeYtelser: String? = null,
        var ytelserTilUtdanning: String? = null,
        var forsorgelseBarn: String? = null
)

//4.4 Ytterligere informasjon om ektefellen
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

//4.5 Ytterligere inforrmasjon om barn av forsikrede
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
        var belop: List<Belop>?
)

data class AndreRessurserInntektskilder (
        var andreRessurserInntektskilder: String? = null,
        var andreRessurserInntektskilderType: String? = null,
        var belop: List<Belop>? = null
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

//4.4.3
data class InntektsgivendeArbeid (
        var inntektsgivendeArbeid: String? = null,
        var belop: List<Belop>? = null
)

//4.4.1 Familiestatus
data class Familiestatus (
        var familestatus: String? = null,
        var datoFamiliestatus: String? = null
)

//4.2.2
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
        var nasjonaliteter: List<Nasjonalitet>? = null
)

data class Nasjonalitet (
        var nasjonalitet: String? = null
)


//AdditionalInformationForInvalidityPensionClaim (P3000_NO_6)
data class Ufore (
        var ekstraInfoBarn: List<EkstraInfoBarn>? = null
)

//AdditionalInformationChildrenInsuredPerson
data class EkstraInfoBarn (
        var adresse: Adresse? = null,
        var barn: Person? = null, //AdditionalInformationChildInsuredPerson
        var barnBorMedBeggeForeldre: BarnBorMedBeggeForeldre? = null
)

//DoesChildLiveTogetherWithBothParents
data class BarnBorMedBeggeForeldre (
        var barnBorMedBruker: BarnBorMedBruker? = null,
        var barnForsorgetAvBruker: BarnForsorgetAvBruker? = null,
        var barnBorMedBeggeForeldreIndikator: BarnBorMedBeggeForeldreIndikator? = null
)

data class BarnForsorgetAvBruker (
        var barnForsorgetAvBrukerIndikator: List<BarnFosorgetAvBrukerIndikator>? = null
)

data class BarnFosorgetAvBrukerIndikator (
        var forsorget: String? = null
)

//FillInFollowingIfDoesChildLiveTogether
data class BarnBorMedBruker (
        var barnBorMedBrukerIndikator: List<BarnBorMedBrukerIndikator>? = null,
        var brukerHarOmsorgForBarnIndikator: List<BrukerHarOmsorgForBarnIndikator> //doesInsuredPersonSupportChildIndicator
)

data class BrukerHarOmsorgForBarnIndikator (
        var omsorgForBarn: String? = null
)

//doesChildLiveWithInsuredPersonIndicator
data class BarnBorMedBrukerIndikator (
        var borMedBruker: String? = null
)

//doesChildLiveTogetherWithBothParentsIndicator
data class BarnBorMedBeggeForeldreIndikator (
        var barnBorMedBeggeForeldre: String? = null
)