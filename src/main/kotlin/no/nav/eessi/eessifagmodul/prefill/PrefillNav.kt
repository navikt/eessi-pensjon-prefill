package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PrefillNav(private val preutfyllingPersonFraTPS: PrefillPersonDataFromTPS) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillNav::class.java) }

    private val validseds : List<String> = listOf("P6000","P4000","P2000", "P5000")

    //TODO hva vil avsender ID på RINA være for NAV-PEN?
    //vil dette hentes fra Fasit? eller Rina?
    private val institutionid = "NO:noinst002"
    private val institutionnavn = "NOINST002, NO INST002, NO"

    fun utfyllNav(utfyllingData: PrefillDataModel): Nav {

        //bruker død hvis etterlatt (etterlatt aktoerid fylt ut)
        val brukertps = bruker(utfyllingData)

        //skal denne kjøres hver gang? eller kun under P2000?
        val barnatps = hentBarnaFraTPS(utfyllingData)
        val pensaknr = utfyllingData.penSaksnummer
        val lokalSaksnr = opprettLokalSaknr( pensaknr )

        val nav = Nav(
                barn = barnatps,
                bruker = brukertps,
                //korrekt bruk av eessisak? skal pen-saknr legges ved?
                //eller peker denne til en ekisterende rina-casenr?
                eessisak = lokalSaksnr,
                krav = Krav("2016-01-01")
            )
        logger.debug("[${utfyllingData.getSEDid()}] Sjekker PinID : ${utfyllingData.personNr}")

        //${nav.eessisak}"
        logger.debug("[${utfyllingData.getSEDid()}] Utfylling av NAV data med lokalsaksnr: $pensaknr")
        return nav
    }

    private fun bruker(utfyllingData: PrefillDataModel): Bruker {
        //kan denne utfylling benyttes på alle SED?
        if (validseds.contains(utfyllingData.getSEDid())) {

            //etterlatt pensjon da er dette den avdøde.(ikke levende)
            //etterlatt pensjon da er den levende i pk.3 sed (gjenlevende) (pensjon.gjenlevende)
            if (utfyllingData.isValidEtterlatt()) {
                val pinid = utfyllingData.avdodPersonnr
                val bruker = preutfyllingPersonFraTPS.prefillBruker(pinid)
                logger.debug("Preutfylling Utfylling (avdød) Nav END")
                return bruker
            }
            val pinid = utfyllingData.personNr
            val bruker = preutfyllingPersonFraTPS.prefillBruker(pinid)
            logger.debug("Preutfylling Utfylling Nav END")
            return bruker

        }
        logger.debug("SED er ikke P6000,P2000,P4000,P5000.. - (fyller ut med mock)")
        val brukerfake = Bruker(
                person = Person(
                    fornavn = "F",
                    kjoenn = "k",
                    foedselsdato = "1901-12-01",
                    etternavn = "E"
                )
            )
        return brukerfake
    }

    private fun hentBarnaFraTPS(utfyllingData: PrefillDataModel) :List<BarnItem> {
        if (utfyllingData.getSEDid() != "P2000") {
            logger.debug("Preutfylling barn SKIP not valid SED?")
            return listOf()
        }
        logger.debug("Preutfylling barn START")
        val barnaspin = preutfyllingPersonFraTPS.hentBarnaPinIdFraBruker(utfyllingData.personNr)
        val barna = mutableListOf<BarnItem>()
        barnaspin.forEach {
            val barnBruker = preutfyllingPersonFraTPS.prefillBruker(it)
            logger.debug("Preutfylling barn x..")
            val barn = BarnItem(
                    person = barnBruker.person,
                    far = barnBruker.far,
                    mor = barnBruker.mor,
                    relasjontilbruker = "BARN"
            )
            barna.add(barn)
        }
        logger.debug("Preutfylling barn END")
        return barna.toList()
    }

    /**
     *
     */
    private fun opprettLokalSaknr(pensaknr: String = ""): List<EessisakItem> {
        //Må få hentet ut NAV institusjon avsender fra fasit?
        val lokalsak = EessisakItem(
                institusjonsid = institutionid,
                institusjonsnavn = institutionnavn,
                saksnummer = pensaknr,
                land = "NO"
        )
        return listOf(lokalsak)
    }


}

