package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.Bruker
import no.nav.eessi.eessifagmodul.models.Nav
import no.nav.eessi.eessifagmodul.models.Person
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PreutfyllingNav(private val preutfyllingPersonFraTPS: PreutfyllingPersonFraTPS) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PreutfyllingNav::class.java) }
    val validseds : List<String> = listOf("P2000", "P6000")

    fun utfyllNav(utfylling: UtfyllingData): Nav {

        val brukertps = bruker(utfylling)
        val nav = Nav(
                bruker = Bruker(
                        person = brukertps.person
                )
        )
        return nav
    }

    private fun bruker(utfylling: UtfyllingData): Bruker {

        val sed = utfylling.sed
        logger.debug("SED.sed : ${sed.sed}")

        //kan denn utfylling benyttes på vilke SED?
        if (validseds.contains(sed.sed)) {
            //preutfylling av sed her!

            val pinid = utfylling.hentPinid()
            val bruker = preutfyllingPersonFraTPS.preutfyllBruker(pinid)

            return bruker
        }
        logger.debug("SED er ikke P6000/P2000 -")
        val brukerfake = Bruker(
                person = Person(
                    fornavn = "Fornavn",
                    kjoenn = "f",
                    foedselsdato = "1967-12-01",
                    etternavn = "Etternavn"
                )
            )
        return brukerfake
    }

}

