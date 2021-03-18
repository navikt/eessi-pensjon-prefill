package no.nav.eessi.pensjon.fagmodul.sedmodel

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Nav(
        val eessisak: List<EessisakItem>? = null,
        val bruker: Bruker? = null,
        val ektefelle: Ektefelle? = null,
        val barn: List<BarnItem>? = null, //pkt 6 og 8
        val verge: Verge? = null,
        val krav: Krav? = null,

        //X005
        val sak: Navsak? = null,
        //P10000 hvordan få denne til å bli val?
        var annenperson: Bruker? = null,
)

//X005
data class Navsak (
        val kontekst: Kontekst? = null,
        val leggtilinstitusjon: Leggtilinstitusjon? = null
)

//X005
data class Kontekst(
        val bruker: Bruker? = null
)

//X005
@JsonIgnoreProperties(ignoreUnknown = true)
data class Leggtilinstitusjon(
        val institusjon: InstitusjonX005? = null,
)

//X005
data class InstitusjonX005(
        val id: String,
        val navn: String
)

data class Krav(
        var dato: String? = null,
        //P15000
        val type: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Bruker(
        val mor: Foreldre? = null,
        val far: Foreldre? = null,
        val person: Person? = null,
        val adresse: Adresse? = null,
        val arbeidsforhold: List<ArbeidsforholdItem>? = null,
        val bank: Bank? = null
)

data class Bank(
        val navn: String? = null,
        val konto: Konto? = null,
        val adresse: Adresse? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Konto(
        val sepa: Sepa? = null,
        val innehaver: Innehaver? = null,
)

data class Sepa(
        val iban: String? = null,
        val swift: String? = null
)

data class Innehaver(
        val rolle: String? = null,
        val navn: String? = null
)

data class Foreldre(
        val person: Person
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BarnItem(
        val mor: Foreldre? = null,
        val person: Person? = null,
        val far: Foreldre? = null,
        val relasjontilbruker: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Ektefelle(
        val person: Person? = null,
        val type: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Verge(
        val person: Person? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Person(
        val pin: List<PinItem>? = null,
        var pinland: PinLandItem? = null, //for H020 og H021
        val statsborgerskap: List<StatsborgerskapItem>? = null, //nasjonalitet
        val etternavn: String? = null,
        val fornavn: String? = null,
        val kjoenn: String? = null,
        val foedested: Foedested? = null,
        val foedselsdato: String? = null,
        val sivilstand: List<SivilstandItem>? = null,   //familiestatus
        val relasjontilavdod: RelasjonAvdodItem? = null, //5.2.5 P2100
        //noe enkel måte å få denne til å forbli val?
        var rolle: String? = null  //3.1 i P10000
)

data class PinLandItem(
        val oppholdsland: String? = null,
        val kompetenteuland: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RelasjonAvdodItem(
        val relasjon: String? = null,  //5.2.5  P2100
)

data class SivilstandItem(
        val fradato: String? = null,
        val status: String? = null
)

data class StatsborgerskapItem(
        val land: String? = null
)

data class ArbeidsforholdItem(
        val inntekt: List<InntektItem?>? = null,
        val planlagtstartdato: String? = null,
        val arbeidstimerperuke: String? = null,
        val planlagtpensjoneringsdato: String? = null,
        val yrke: String? = null,
        val type: String? = null,
        val sluttdato: String? = null
)

data class InntektItem(
        val betalingshyppighetinntekt: String? = null,
        val beloeputbetaltsiden: String? = null,
        val valuta: String? = null,
        val annenbetalingshyppighetinntekt: String? = null,
        val beloep: String? = null
)

data class PinItem(
        val institusjonsnavn: String? = null,
        val institusjonsid: String? = null,
        val sektor: String? = null,
        val identifikator: String? = null,  //rename? f.eks personnummer
        val land: String? = null,
        //P2000, P2100, P2200
        val institusjon: Institusjon? = null
)

data class Adresse(
        val gate: String? = null,
        val bygning: String? = null,
        val by: String? = null,
        val postnummer: String? = null,
        val region: String? = null,
        val land: String? = null,
        val kontaktpersonadresse: String? = null,
        val datoforadresseendring: String? = null,
        val postadresse: String? = null,
        val startdato: String? = null
)

data class Foedested(
        val by: String? = null,
        val land: String? = null,
        val region: String? = null
)

data class EessisakItem(
        val institusjonsid: String? = null,
        val institusjonsnavn: String? = null,
        val saksnummer: String? = null,
        val land: String? = null
)