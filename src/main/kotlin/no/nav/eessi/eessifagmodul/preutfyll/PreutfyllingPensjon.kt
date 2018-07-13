package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.Bruker
import no.nav.eessi.eessifagmodul.models.Nav
import no.nav.eessi.eessifagmodul.models.Pensjon
import no.nav.eessi.eessifagmodul.models.Person
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PreutfyllingPensjon(private val preutfyllingPersonFraTPS: PreutfyllingPersonFraTPS) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PreutfyllingPensjon::class.java) }

    private val validseds : List<String> = listOf("P2000", "P6000")


    fun pensjon(utfylling: UtfyllingData): Pensjon {

        //min krav for P6000
        val sed = utfylling.sed
       logger.debug("SED.sed : ${sed.sed}")

        //validere om vi kan preutfylle for angitt SED
        if (validseds.contains(sed.sed)) {

            val pinid = utfylling.hentPinid()



            val brukertps = preutfyllingPersonFraTPS.preutfyllBruker(pinid)
            val pensjon = Pensjon(
                      gjenlevende = brukertps
            )
            return pensjon
        }

        logger.debug("SED er ikke P6000/P2000 -")
        val pensjonfake = Pensjon(
            gjenlevende = Bruker(
                person = Person(
                    fornavn = "Fornavn",
                    kjoenn = "f",
                    foedselsdato = "1967-12-01",
                    etternavn = "Etternavn"
                )
            )
        )
        return pensjonfake
    }

}