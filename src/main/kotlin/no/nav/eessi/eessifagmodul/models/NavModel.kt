package no.nav.eessi.eessifagmodul.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NAV objekt
 */
data class Nav(
   val ektefelle: Ektefelle? = null,
   val barn: List<BarnItem>? = null,
   var bruker: Bruker? = null,
   @JsonProperty("eessisak")
   var eessisak: List<EessisakItem>? = null,
   val verge: Verge? = null

)
data class Bruker(
        @JsonProperty("arbeidsforhold")
        val arbeidsforhold: List<ArbeidsforholdItem>? = null,
        val bank: Bank? = null,
        @JsonProperty("mor")
        var mor: Foreldre? = null,
        @JsonProperty("far")
        var far: Foreldre? = null,
        var person: Person? = null,
        var adresse: Adresse? = null

)

data class Bank(
        val konto: Konto? = null
)
data class Konto(
        val sepa: Sepa? = null,
        val innehaver: Innehaver? = null
)
data class Sepa(
        val iban: String? = null,
        val swift: String? = null
)
data class Innehaver(
        val rolle: String? = null,
        val navn: String? = null
)

//MOR - FAR
data class Foreldre(
        var person: Person? = null
)

data class BarnItem(
        @JsonProperty("mor")
        val mor: Foreldre? = null,
        @field:JsonProperty("person")
        val person: Person? = null,
        @field:JsonProperty("far")
        val far: Foreldre? = null,
        @JsonProperty("opplysningeromannetbarn")
        val opplysningeromannetbarn: String? = null,
        @JsonProperty("relasjontilbruker")
        val relasjontilbruker: String? = null
)

data class Ektefelle(
        @field:JsonProperty("mor")
        val mor: Foreldre? = null,
        val person: Person? = null,
        @field:JsonProperty("far")
        val far: Foreldre? = null,
        val type: String? = null
)

data class Verge(
        val person: Person? = null,
        val adresse: Adresse? = null,
        val vergemaal: Vergemaal? = null
)
data class Vergemaal(
        val mandat: String? = null
)

data class Kontakt(
       val telefon: List<TelefonItem>? = null,
       val email: List<EmailItem>? = null
)
data class TelefonItem(
       val type: String? = null,
       val nummer: String? = null
)
data class EmailItem(
        val adresse: String? = null
)


data class Person(
        @JsonProperty("pin")
        var pin: List<PinItem>? = null,
        var pinannen: PinItem? = null,
        @JsonProperty("statsborgerskap")
        var statsborgerskap: List<StatsborgerskapItem>? = null,
        var etternavn: String? = null,
        var fornavn: String? = null,
        var kjoenn: String? = null,
        var foedested: Foedested? = null,
        var tidligerefornavn: String? = null,
        var tidligereetternavn: String? = null,
        var forrnavnvedfoedsel: String? = null,
        var etternavnvedfoedsel: String? = null,
        var foedselsdato: String? = null,
        val doedsdato: String? = null,

        val kontakt: Kontakt? = null,
        val sivilstand: List<SivilstandItem>? = null

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
        val sektor: String? = null,
        val identifikator: String? = null,
        val land: String? = null
)

data class Adresse(
        val postnummer: String? = null,
        val by: String? = null,
        val bygning: String? = null,
        val land: String? = null,
        val gate: String? = null,
        val region: String? = null
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

data class Institusjonsadresse(
        val poststed: String? = null,
        val postnummer: String? = null,
        val land: String? = null
)

data class Ignore(
        val buildingName: String? = null,
        val region: String? = null,
        val otherInformation: String? = null
)

//End Nav class