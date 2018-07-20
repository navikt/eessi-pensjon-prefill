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
    private val validseds : List<String> = listOf("P6000")

    fun pensjon(utfyllingData: UtfyllingData): Pensjon {

        //min krav for P6000
        val sed = utfyllingData.hentSED()
       logger.debug("SED.sed : ${sed.sed}")

        //validere om vi kan preutfylle for angitt SED
        if (validseds.contains(sed.sed)) {
            //norskident pnr.
            val pinid = utfyllingData.hentPinid()
            val pensjon = Pensjon(gjenlevende = preutfyllingPersonFraTPS.preutfyllBruker(pinid!!)
            )
            logger.debug("Preutfylling Utfylling Pensjon END")
            return pensjon
        }

        logger.debug("SED er ikke P6000")
        val pensjonfake = Pensjon(
            gjenlevende = Bruker(
                person = Person(
                    fornavn = "F",
                    kjoenn = "k",
                    foedselsdato = "1901-12-01",
                    etternavn = "E"
                )
            )
        )
        return pensjonfake
    }

}