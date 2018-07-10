package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PreutfyllingNav(val utfylling: Utfylling) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PreutfyllingNav::class.java) }


    fun utfyllNav(): Nav {

        val nav = Nav()
        nav.bruker = bruker()
        return nav
    }

    private fun bruker(): Bruker {

        //min krav for P6000
        //sed.nav = Nav(
        val sed = utfylling.sed

        logger.debug("SED.sed : ${sed.sed}")

        if (validSED("P6000", sed)) {
            logger.debug("Sjekk om SED er P6000")
            val bruker = Bruker(
                    person = Person(
                            fornavn = "Fornavn",
                            kjoenn = "f",
                            foedselsdato = "1957-12-01",
                            etternavn = "Etternavn"
                    )
            )

            //preutfylling
            val grad = Grad(grad = 100, felt = "Bruker", beskrivelse = "Bruker/Person")
            utfylling.leggtilGrad(grad)
            utfylling.leggtilTjeneste("Preutfylling/P6000")

            return bruker

        }
        logger.debug("SED er ikke P6000")

        //Ingen preutfylling
        utfylling.leggtilGrad(Grad(grad = 0, felt = "Bruker", beskrivelse = "Bruker"))
        utfylling.leggtilTjeneste("Preutfylling/N/A")
        return Bruker()
    }

}

