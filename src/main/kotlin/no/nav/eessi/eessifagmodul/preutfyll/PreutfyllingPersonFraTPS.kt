package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.clients.personv3.PersonV3Client
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.models.Bruker
import no.nav.eessi.eessifagmodul.models.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat

@Service
class PreutfyllingPersonFraTPS(private val personV3Client: PersonV3Client) {

    private val dateformat = "YYYY-MM-dd"

    fun preutfyllBruker(ident: String): Bruker {

        val response =  personV3Client.hentPerson(ident)
        val personTps : no.nav.tjeneste.virksomhet.person.v3.informasjon.Person = response.person

        val tpsBruker = Bruker(
            person = personData(personTps),
            adresse = personAdresse(personTps)
        )
        return tpsBruker
    }

    private fun personData(personTps: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): Person {
        val navn = personTps.personnavn
        val persident = personTps.aktoer as PersonIdent
        val pinid = persident.ident
        val statsborgerskap = personTps.statsborgerskap
        val fdato = personTps.foedselsdato
        val kjonn = personTps.kjoenn
        val fodCalendar = fdato.foedselsdato.toGregorianCalendar()
        val fodseldatoformatert = SimpleDateFormat(dateformat).format(fodCalendar.time)

        val person = Person(
                pin = listOf(PinItem( sektor = "alle", identifikator = pinid.ident, land = formatNORland(statsborgerskap.land.value))),
                //etternavnvedfoedsel = navn.etternavn,
                forrnavnvedfoedsel = navn.fornavn,
                fornavn = navn.fornavn,
                etternavn = navn.etternavn,
                foedselsdato = fodseldatoformatert,
                statsborgerskap = listOf(statsBorgerskap(personTps)),
                kjoenn = mapKjonn(kjonn.kjoenn.value)
        )

        return person
    }

    private fun personAdresse(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): Adresse{
        val gateAdresse = person.bostedsadresse.strukturertAdresse as Gateadresse

        val adr = Adresse(
            postnummer = gateAdresse.poststed.value,
            region = gateAdresse.kommunenummer,
            gate = gateAdresse.gatenavn,
            bygning = gateAdresse.husnummer.toString(),
            land = formatNORland(gateAdresse.landkode.value)
        )
        return adr
    }

    private fun statsBorgerskap(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): StatsborgerskapItem {
        val statsborgerskap = person.statsborgerskap
        val statitem = StatsborgerskapItem(
            land = formatNORland(statsborgerskap.land.value)
        )
        return statitem
    }

    //Midlertidig funksjon for map TPS til EUX/Rina
    private fun formatNORland(land: String): String {
        val map: Map<String, String> = hashMapOf("NOR" to "NO")
        return map.get(land)!!
    }

    //Midlertidig funksjon for map TPS til EUX/Rina
    private fun mapKjonn(kjonn: String): String {
        val map: Map<String, String> = hashMapOf("M" to "m", "K" to "f")
        val value = map.get(kjonn)
        return  value ?: "u"
    }



}