package no.nav.eessi.eessifagmodul.models

import com.fasterxml.jackson.annotation.JsonProperty


/**
 * NAV objekt
 */
data class Nav(
        var bruker: Bruker? = null,
        @JsonProperty("eessisak")
        var eessisak: List<EessisakItem>? = null
)

data class Bruker(
        var mor: Mor? = null,
        var person: Person? = null,
        var far: Far? = null,
        var adresse: Adresse? = null
)

data class Far(
        var person: Person? = null
)

data class Mor(
        var person: Person? = null
)

data class Person(
        @JsonProperty("pin")
        var pin: List<PinItem>? = null,
        var pinannen: PinItem? = null,
        var etternavnvedfoedsel: String? = null,
        @JsonProperty("statsborgerskap")
        var statsborgerskap: List<StatsborgerskapItem>? = null,
        var etternavn: String? = null,
        var kjoenn: String? = null,
        var tidligereetternavn: String? = null,
        var foedested: Foedested? = null,
        var foedselsdato: String? = null,
        var fornavn: String? = null,
        var tidligerefornavn: String? = null,
        var forrnavnvedfoedsel: String? = null
)
data class StatsborgerskapItem(
        val land: String? = null
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

data class Gjenlevende(
        val mor: Mor? = null,
        val far: Far? = null,
        val person: Person? = null,
        val adresse: Adresse? = null
)

