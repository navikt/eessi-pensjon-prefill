package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.clients.personv3.PersonV3Client
import no.nav.eessi.eessifagmodul.services.LandkodeService
import no.nav.eessi.eessifagmodul.services.PostnummerService
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.models.Bruker
import no.nav.eessi.eessifagmodul.models.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat

@Service
class PreutfyllingPersonFraTPS(private val personV3Client: PersonV3Client, private val postnummerService: PostnummerService, private val landkoder: LandkodeService) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PreutfyllingPersonFraTPS::class.java) }
    private val dateformat = "YYYY-MM-dd"

    private enum class ForeldreEnum(val foreldre: String) {
        FAR("FARA"),
        MOR("MORA");
        fun erSamme(foreldreTPS: String): Boolean {
            return foreldre.equals(foreldreTPS)
        }
    }

    fun preutfyllBruker(ident: String): Bruker {

        val brukerTPS = hentBrukerTPS(ident)

        val bruker = Bruker(
            far = hentForeldre(ForeldreEnum.FAR, brukerTPS),
            mor = hentForeldre(ForeldreEnum.MOR, brukerTPS),
            person = personData(brukerTPS),
            adresse = personAdresse(brukerTPS)
        )
        logger.debug("Preutfylling Bruker")
        return bruker
    }

    //bruker fra TPS
    private fun hentBrukerTPS(ident: String): no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker {
        val response =  personV3Client.hentPerson(ident)
        return response.person as no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker
    }

    //personnr
    private fun hentNorIdent(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): String {
        val persident = person.aktoer as PersonIdent
        val pinid: NorskIdent = persident.ident
        return pinid.ident
    }

    //fdato i rinaformat
    private fun datoFormat(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): String {
        val fdato = person.foedselsdato
        val fodCalendar = fdato.foedselsdato.toGregorianCalendar()
        val fodseldatoformatert = SimpleDateFormat(dateformat).format(fodCalendar.time)
        return fodseldatoformatert
    }

    fun hentFodested(bruker: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker): Foedested {
        val fstedTPS = bruker.foedested
        val fsted = Foedested(
                land = fstedTPS ?: "Unknown",
                by = "Unkown",
                region = "Unknown"
        )
        if (fsted.land == "Unknown") {
            return Foedested()
        }

        return fsted
    }

    //mor / far
    private fun hentForeldre(relasjon: ForeldreEnum, person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): Foreldre? {
        person.harFraRolleI.forEach {
            val tpsvalue = it.tilRolle.value
            if (relasjon.erSamme(tpsvalue)) {
                val persontps = it.tilPerson as no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
                val navntps = persontps.personnavn as Personnavn
                val foreldreperson = Person(
                    fornavn = navntps.fornavn,
                    etternavnvedfoedsel = navntps.etternavn
                )
                if (ForeldreEnum.MOR.erSamme(tpsvalue)) {
                    foreldreperson.etternavnvedfoedsel = ""
                }
                logger.debug("Preutfylling Foreldre")
                return Foreldre(foreldreperson)
            }
        }
        return null
    }

    //persondata - rina format
    private fun personData(brukerTps: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker): Person {
        val navn = brukerTps.personnavn as Personnavn
        val statsborgerskap = brukerTps.statsborgerskap as Statsborgerskap
        val kjonn = brukerTps.kjoenn

        val person = Person(
                pin = listOf(
                        PinItem(
                        sektor = "alle",
                        identifikator = hentNorIdent(brukerTps),
                        land = hentLandkode(statsborgerskap.land)
                        )
                    ),
                forrnavnvedfoedsel = navn.fornavn,
                fornavn = navn.fornavn,
                etternavn = navn.etternavn,
                foedselsdato = datoFormat(brukerTps),
                statsborgerskap = listOf(statsBorgerskap(brukerTps)),
                kjoenn = mapKjonn(kjonn),
                foedested = hentFodested(brukerTps)
        )
        logger.debug("Preutfylling Person")
        return person
    }

    fun personAdresse(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): Adresse{
        val gateAdresse = person.bostedsadresse.strukturertAdresse as Gateadresse
        val postnr = gateAdresse.poststed.value
        val adr = Adresse(
            postnummer = postnr,
            region = gateAdresse.kommunenummer,
            gate = gateAdresse.gatenavn,
            bygning = gateAdresse.husnummer.toString(),
            land = hentLandkode(gateAdresse.landkode),
            by = postnummerService.finnPoststed(postnr)
        )
        logger.debug("Preutfylling by(sted) benytter finnPoststed")
        logger.debug("Preutfylling Gateadresse")
        return adr
    }

    private fun statsBorgerskap(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): StatsborgerskapItem {
        val statsborgerskap = person.statsborgerskap as Statsborgerskap
        val land = statsborgerskap.land as no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder
        val statitem = StatsborgerskapItem(
            land = hentLandkode(land)
        )
        logger.debug("Preutfylling Statsborgerskap")
        return statitem
    }

    //Denne blir vel flyttet til Basis når mapping blir rettet opp fra NO=NO til NOR=NO (TPS/EU-RINA)??
    private fun hentLandkode(landkodertps: no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder): String? {
        val result = landkoder.finnLandkode2(landkodertps.value)
        logger.debug("Preutfylling mapping Landkode (alpha3-alpha2)  ${landkodertps.value} til $result")
        return result
    }

    //Midlertidige - mapping i Basis vil bli rettet slik at det sammkjører mot tps mapping.
    //Midlertidig funksjon for map TPS til EUX/Rina
    private fun mapKjonn(kjonn: Kjoenn): String {
        val ktyper = kjonn.kjoenn
        val map: Map<String, String> = hashMapOf("M" to "m", "K" to "f")
        val value = map[ktyper.value]
        return  value ?: "u"
    }



}