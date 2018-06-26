package no.nav.eessi.eessifagmodul.models

import com.fasterxml.jackson.annotation.JsonProperty

data class Nav(
        val bruker: Bruker? = null,
        @JsonProperty("eessisak")
        val eessisak: List<EessisakItem>? = null
)

data class Bruker(
        val mor: Mor? = null,
        val person: Person? = null,
        val far: Far? = null,
        val adresse: Adresse? = null
)

data class Far(
        val person: Person? = null
)

data class Mor(
        val person: Person? = null
)

data class Person(
        @JsonProperty("pin")
        val pin: List<PinItem>? = null,
        val pinannen: PinItem? = null,
        val etternavnvedfoedsel: String? = null,
        @JsonProperty("statsborgerskap")
        val statsborgerskap: List<StatsborgerskapItem>? = null,
        val etternavn: String? = null,
        val kjoenn: String? = null,
        val tidligereetternavn: String? = null,
        val foedested: Foedested? = null,
        val foedselsdato: String? = null,
        val fornavn: String? = null,
        val tidligerefornavn: String? = null,
        val forrnavnvedfoedsel: String? = null
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

//data class Tillegsinformasjon(
//        val saksnummer: String? = null
//)

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

/**
 * Mock class genererer mock/fake Nav objekt
 */
class NavMock {

    /**
     * genererer mock av Nav
     * @return Nav
     */
    fun genererNavMock(): Nav {
        return Nav(
                bruker = Bruker(
                        mor = Mor(
                                Person(
                                        etternavnvedfoedsel = "asdfsdf",
                                        fornavn = "asfsdf")),
                        person = Person(
                                pin = listOf(
                                        PinItem(
                                                sektor = "pensjoner",
                                                land = "BG",
                                                identifikator = "weqrwerwqe"
                                        ),
                                        PinItem(
                                                sektor = "alle",
                                                land = "NO",
                                                identifikator = "01126712345"
                                        )
                                )
                                ,
                                fornavn = "Gul",
                                kjoenn = "f",
                                foedselsdato = "1967-12-01",
                                etternavn = "Konsoll",
                                tidligereetternavn = "sdfsfasdf",
                                statsborgerskap = listOf(
                                        StatsborgerskapItem("BE"),
                                        StatsborgerskapItem("BG"),
                                        StatsborgerskapItem("GR"),
                                        StatsborgerskapItem("GB")
                                ),
                                foedested = Foedested(
                                        region = "sfgdfdgs",
                                        land = "DK",
                                        by = "gafdgsf"
                                ),
                                forrnavnvedfoedsel = "werwerwe",
                                tidligerefornavn = "asdfdsffsd",
                                etternavnvedfoedsel = "werwreq"
                        ),
                        far = Far(
                                Person(
                                        etternavnvedfoedsel = "safasfsd",
                                        fornavn = "farfornavn"
                                )
                        )
                ),
                eessisak = listOf(
                        EessisakItem(
                                saksnummer = "24234sdsd-4",
                                land = "NO"
                        ),
                        EessisakItem(
                                saksnummer = "retretretert",
                                land = "HR"
                        )
                )
        )
    }

}