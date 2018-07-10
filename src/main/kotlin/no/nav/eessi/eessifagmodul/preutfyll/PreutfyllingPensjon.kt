package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PreutfyllingPensjon(val utfylling: Utfylling){

    private val logger: Logger by lazy { LoggerFactory.getLogger(PreutfyllingPensjon::class.java) }

    fun pensjon(): Pensjon {

        //min krav for P6000
        val sed = utfylling.sed

        logger.debug("SED.sed : ${sed.sed}")

        if (validSED("P6000",sed)) {
            val pensjon = Pensjon(
                    gjenlevende = Gjenlevende(
                            person = Person(
                                    fornavn = "Fornavn",
                                    kjoenn = "f",
                                    foedselsdato = "1967-12-01",
                                    etternavn = "Etternavn"
                            )
                    )
            )
            //preutfylling
            val grad = Grad(grad = 100, felt = "Pensjon", beskrivelse = "Pensjon/Person")
            val tjenester = "Preutfylling/P6000"
            utfylling.leggtilGrad(grad)
            utfylling.leggtilTjeneste(tjenester)
            return pensjon
        } else {

            //Ingen preutfylling
            utfylling.leggtilGrad(Grad(grad = 0, felt = "Pensjon", beskrivelse = "Pensjon"))
            utfylling.leggtilTjeneste("Preutfylling/N/A")

            return Pensjon()
        }
    }

}