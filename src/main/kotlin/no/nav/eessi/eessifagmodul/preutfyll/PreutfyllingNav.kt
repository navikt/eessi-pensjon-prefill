package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.Bruker
import no.nav.eessi.eessifagmodul.models.EessisakItem
import no.nav.eessi.eessifagmodul.models.Nav
import no.nav.eessi.eessifagmodul.models.Person
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PreutfyllingNav(private val preutfyllingPersonFraTPS: PreutfyllingPersonFraTPS) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PreutfyllingNav::class.java) }
    val validseds : List<String> = listOf("P6000","P4000")

    fun utfyllNav(utfyllingData: UtfyllingData): Nav {

        val brukertps = bruker(utfyllingData)
        val nav = Nav(
                bruker = Bruker(
                        person = brukertps.person
                ),
                //korrekt bruk av eessisak? skal pen-saknr legges ved?
                //eller peker denne til en ekisterende rina-casenr?
//                eessisak = listOf(
//                        EessisakItem(
//                                saksnummer = "PEN-SAK:" + utfyllingData.hentSaksnr(),
//                                land = "NO"
//                        )
//                ),
                eessisak = utfyllingData.hentSaksnr()?.let { opprettLokalSaknr(it) }
        )
        logger.debug("Utfylling av NAV data med lokalsaksnr: ${nav.eessisak}")
        return nav
    }

    private fun bruker(utfyllingData: UtfyllingData): Bruker {

        val sed = utfyllingData.hentSED()
        logger.debug("SED.sed : ${sed.sed}")

        //kan denne utfylling benyttes på alle SED?
        if (validseds.contains(sed.sed)) {

            //preutfyll av sed her!
            val pinid = utfyllingData.hentPinid()
            val bruker = preutfyllingPersonFraTPS.preutfyllBruker(pinid!!)

            logger.debug("Preutfylling Utfylling Nav END")
            return bruker
        }

        logger.debug("SED er ikke P6000 - (fyller ut med mock)")
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

    private fun opprettLokalSaknr(pensaknr: String = ""): List<EessisakItem> {
        val lokalsak = EessisakItem(
                institusjonsid = "NO:noinst002",
                institusjonsnavn = "NOINST002, NO INST002, NO",
                saksnummer = "PEN-SAK: $pensaknr",
                land = "NO"
        )
        return listOf<EessisakItem>(lokalsak)
    }


}

