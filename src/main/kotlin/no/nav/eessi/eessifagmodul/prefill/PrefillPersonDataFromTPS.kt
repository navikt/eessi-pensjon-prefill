package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.models.Bruker
import no.nav.eessi.eessifagmodul.models.Person
import no.nav.eessi.eessifagmodul.services.LandkodeService
import no.nav.eessi.eessifagmodul.services.PostnummerService
import no.nav.eessi.eessifagmodul.services.personv3.PersonV3Service
import no.nav.eessi.eessifagmodul.utils.simpleFormat
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PrefillPersonDataFromTPS(private val personV3Service: PersonV3Service,
                               private val postnummerService: PostnummerService,
                               private val landkodeService: LandkodeService) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPersonDataFromTPS::class.java) }
    private val dod = "DØD"

    private var personstatus = ""

    private enum class RelasjonEnum(val relasjon: String) {
        FAR("FARA"),
        MOR("MORA"),
        BARN("BARN");

        fun erSamme(relasjonTPS: String): Boolean {
            return relasjon == relasjonTPS
        }
    }

    //henter kun personnNr (brukerNorIdent/pin) for alle barn under person
    fun hentBarnaPinIdFraBruker(brukerNorIdent: String): List<String> {
        val brukerTPS = hentBrukerTPS(brukerNorIdent)
        val person = brukerTPS as no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
        val resultat = mutableListOf<String>()
        person.harFraRolleI.forEach {
            val tpsvalue = it.tilRolle.value   //mulig nullpoint? kan tilRolle være null?

            if (RelasjonEnum.BARN.erSamme(tpsvalue)) {
                val persontps = it.tilPerson as no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
                //henter kun ut norskident ut
                resultat.add(hentNorIdent(persontps))
                logger.debug("Preutfylling barn NorIdent")
            }
        }
        return resultat.toList()
    }

    fun prefillBruker(ident: String): Bruker {
        logger.debug("         Bruker")
        val brukerTPS = hentBrukerTPS(ident)

        setPersonStatus(hentPersonStatus(brukerTPS))

        val bruker = Bruker(
                person = personData(brukerTPS),

                far = Foreldre(person = hentRelasjon(RelasjonEnum.FAR, brukerTPS)),

                mor = Foreldre(person = hentRelasjon(RelasjonEnum.MOR, brukerTPS)),


                adresse = personAdresse(brukerTPS)
        )
        logger.debug("Preutfylling Bruker")
        return bruker
    }

    private fun setPersonStatus(status: String = "") {
        this.personstatus = status
    }

    private fun getPersonStatus(): String {
        return this.personstatus
    }

    private fun validatePersonStatus(value: String): Boolean {
        return getPersonStatus() == value
    }

    //bruker fra TPS
    private fun hentBrukerTPS(ident: String): no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker {
        val response = personV3Service.hentPerson(ident)
        return response.person as no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker
    }

    //personnr
    private fun hentNorIdent(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): String {
        logger.debug("2.1.7.1.2     Personal Identification Number (PIN) personnr")
        val persident = person.aktoer as PersonIdent
        val pinid: NorskIdent = persident.ident
        return pinid.ident
    }

    //fdato i rinaformat
    private fun datoFormat(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): String? {
        logger.debug("2.1.3     Date of birth")
        val fdato = person.foedselsdato
        return fdato?.foedselsdato?.simpleFormat()
    }

    //doddato i rina
    private fun dodDatoFormat(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): String? {
        logger.debug("4.2     Date of death / dødsdato P2100")
        val doddato = person.doedsdato
        return doddato?.doedsdato?.simpleFormat()
    }

    fun hentFodested(bruker: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker): Foedested {
        logger.debug("2.1.8.1       Fødested")

        val fsted = Foedested(
                land = bruker.foedested ?: "Unknown",
                by = "Unkown",
                region = ""
        )
        if (fsted.land == "Unknown") {
            return Foedested()
        }
        return fsted
    }

    //mor / far
    private fun hentRelasjon(relasjon: RelasjonEnum, person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): Person? {
        person.harFraRolleI.forEach {
            val tpsvalue = it.tilRolle.value
            if (relasjon.erSamme(tpsvalue)) {
                logger.debug("Finner relasjon til : $tpsvalue")
                val persontps = it.tilPerson as no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
                val land: String? = try {
                    if (persontps.statsborgerskap != null) {
                        val statsborgerskap = persontps.statsborgerskap as Statsborgerskap
                        hentLandkode(statsborgerskap.land)
                    } else {
                        null
                    }
                } catch (ex: NullPointerException) {
                    logger.debug(ex.message)
                    null
                }
                logger.debug("har vi hentet ut land rett: $land")
                val navntps = persontps.personnavn as Personnavn
                val relasjonperson = Person(
                        pin = listOf(
                                PinItem(
                                        sektor = "alle",
                                        identifikator = hentNorIdent(persontps),
                                        land = land
                                )
                        ),
                        fornavn = navntps.fornavn,
                        etternavnvedfoedsel = navntps.etternavn,
                        doedsdato = dodDatoFormat(persontps)
                )
                if (RelasjonEnum.MOR.erSamme(tpsvalue)) {
                    relasjonperson.etternavnvedfoedsel = null
                }
                logger.debug("Preutfylling Foreldre")
                return relasjonperson
            }
        }
        return null
    }

    //persondata - nav-sed format
    private fun personData(brukerTps: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker): Person {
        logger.debug("2         Persondata (forsikret person)")
        val navn = brukerTps.personnavn as Personnavn
        val kjonn = brukerTps.kjoenn

        val person = Person(

                pin = hentPersonPinNorIdent(brukerTps),

                fornavnvedfoedsel = navn.fornavn,

                //2.1.2     forname
                fornavn = navn.fornavn,

                //2.1.1     familiy name
                etternavn = navn.etternavn,

                //2.1.3
                foedselsdato = datoFormat(brukerTps),

                //2.1.7
                statsborgerskap = listOf(statsBorgerskap(brukerTps)),

                //2.1.4     //sex
                kjoenn = mapKjonn(kjonn),

                //2.1.8.1           place of birth
                foedested = hentFodested(brukerTps),


                sivilstand = hentSivilstand(brukerTps)
        )
        //logger.debug("Preutfylling Person")
        return person
    }

    private fun hentPersonPinNorIdent(brukerTps: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker): List<PinItem> {
        logger.debug("2.1.7.1       Personnummer")
        return listOf(
                PinItem(
                        //all sector
                        sektor = "03",

                        //personnr
                        identifikator = hentNorIdent(brukerTps),

                        // norsk personnr alltid NO
                        land = "NO"
                )
        )
    }

    private fun hentPersonStatus(brukerTps: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker): String {
        val personstatus = brukerTps.personstatus as Personstatus
        return personstatus.personstatus.value
    }

    //sivilstand ENKE, PENS,
    private fun hentSivilstand(brukerTps: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker): List<SivilstandItem> {
        logger.debug("x.x.x         Sivilstand")
        val sivilstand = brukerTps.sivilstand as Sivilstand
        val sivil = SivilstandItem(
                //fradato = standardDatoformat(sivilstand.fomGyldighetsperiode),
                fradato = sivilstand.fomGyldighetsperiode.simpleFormat(),
                status = sivilstand.sivilstand.value
        )
        return listOf(sivil)
    }


    //2.2.2
    fun personAdresse(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): Adresse {
        logger.debug("2.2.2         Adresse")

        //ikke adresse for død
        if (validatePersonStatus(dod)) {
            logger.debug("2.2.2         Ingenting på avdod")
            return Adresse()
        }

        //Gateadresse eller UstrukturertAdresse
        val bostedsadresse: Bostedsadresse = person.bostedsadresse ?: return personAdresseUstrukturert(person.postadresse)
        val gateAdresse = bostedsadresse.strukturertAdresse as Gateadresse

        val gate = gateAdresse.gatenavn
        val husnr = gateAdresse.husnummer

        return Adresse(
                postnummer = gateAdresse.poststed.value,

                gate = "$gate $husnr",

                land = hentLandkode(gateAdresse.landkode),

                by = postnummerService.finnPoststed(gateAdresse.poststed.value)
        )
    }

    private fun personAdresseUstrukturert(postadr: no.nav.tjeneste.virksomhet.person.v3.informasjon.Postadresse): Adresse {
        logger.debug("2.2.2             UstrukturertAdresse (utland)")
        val gateAdresse = postadr.ustrukturertAdresse as UstrukturertAdresse

        return Adresse(
                bygning = gateAdresse.adresselinje1,

                gate = gateAdresse.adresselinje2,

                postnummer = gateAdresse.adresselinje3,

                by = gateAdresse.adresselinje4,

                land = hentLandkode(gateAdresse.landkode)
        )
    }

    private fun statsBorgerskap(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): StatsborgerskapItem {
        logger.debug("2.1.7.1.1     Land / Statsborgerskap")
        val statsborgerskap = person.statsborgerskap as Statsborgerskap
        val land = statsborgerskap.land as no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder
        val statitem = StatsborgerskapItem(
                land = hentLandkode(land)
        )
        return statitem
    }

    //TODO: Mapping av landkoder skal gjøres i codemapping i EUX
    private fun hentLandkode(landkodertps: no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder): String? {
        val result = landkodeService.finnLandkode2(landkodertps.value)
        //logger.debug("Preutfylling Landkode (alpha3-alpha2)  ${landkodertps.value} til $result")
        return result
    }

    //TODO: Mapping av kjønn skal defineres i codemapping i EUX
    private fun mapKjonn(kjonn: Kjoenn): String {
        logger.debug("2.1.4         Kjønn")
        val ktyper = kjonn.kjoenn
        val map: Map<String, String> = hashMapOf("M" to "m", "K" to "f")
        val value = map[ktyper.value]
        return value ?: "u"
    }


}