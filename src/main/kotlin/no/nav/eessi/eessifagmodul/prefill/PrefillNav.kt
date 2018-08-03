package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PrefillNav(private val preutfyllingPersonFraTPS: PrefillPersonDataFromTPS) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillNav::class.java) }
    val validseds : List<String> = listOf("P6000","P4000","P2000")

    fun utfyllNav(utfyllingData: PrefillDataModel): Nav {

        val brukertps = bruker(utfyllingData)
        val barnatps = hentBarnaFraTPS(utfyllingData)

        val nav = Nav(
                barn = barnatps,
//                bruker = Bruker(
//                        person = brukertps.person
//                ),
                bruker = brukertps,
                //korrekt bruk av eessisak? skal pen-saknr legges ved?
                //eller peker denne til en ekisterende rina-casenr?
                eessisak = opprettLokalSaknr( utfyllingData.hentSaksnr() )
        )

        logger.debug("Utfylling av NAV data med lokalsaksnr: ${nav.eessisak}")
        return nav
    }

    private fun bruker(utfyllingData: PrefillDataModel): Bruker {

        val sed = utfyllingData.hentSED()
        logger.debug("SED.sed : ${sed.sed}")

        //kan denne utfylling benyttes på alle SED?
        if (validseds.contains(sed.sed)) {

            //prefill av sed her!
            val pinid = utfyllingData.hentPinid()
            val bruker = preutfyllingPersonFraTPS.prefillBruker(pinid)

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

    private fun hentBarnaFraTPS(utfyllingData: PrefillDataModel) :List<BarnItem> {
        val pinid = utfyllingData.hentPinid()

        val barnaspin = preutfyllingPersonFraTPS.hentBarnaPinIdFraBruker(pinid)

        val barna = mutableListOf<BarnItem>()
        barnaspin.forEach {
            val barnBruker = preutfyllingPersonFraTPS.prefillBruker(it)
            val barn = BarnItem(
                    person = barnBruker.person,
                    far = barnBruker.far,
                    mor = barnBruker.mor,
                    relasjontilbruker = "BARN"
            )
            barna.add(barn)
        }

        return barna.toList()
    }

    private fun opprettLokalSaknr(pensaknr: String = ""): List<EessisakItem> {
        val lokalsak = EessisakItem(
                institusjonsid = "NO:noinst002",
                institusjonsnavn = "NOINST002, NO INST002, NO",
                saksnummer = "PEN-SAK: $pensaknr, VEDTAK: 01234567",
                land = "NO"
        )
        return listOf(lokalsak)
    }


}

