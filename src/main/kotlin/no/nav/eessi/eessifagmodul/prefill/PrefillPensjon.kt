package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.Pensjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PrefillPensjon(private val preutfyllingPersonFraTPS: PrefillPersonDataFromTPS) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPensjon::class.java) }
    private val validseds : List<String> = listOf("P6000", "P4000", "P2000")

    fun pensjon(utfyllingData: PrefillDataModel): Pensjon {

        //min krav for P6000
        val sed = utfyllingData.hentSED()
       logger.debug("SED.sed : ${sed.sed}")

        //validere om vi kan preutfylle for angitt SED
        if (validseds.contains(utfyllingData.hentSEDid())) {
            //norskident pnr.
            val pinid = utfyllingData.hentPinid()

            // er denne person en gjenlevende? hva må da gjøres i nav.bruker.person?
            //
            val pensjon = Pensjon (

                    gjenlevende = preutfyllingPersonFraTPS.prefillBruker(pinid!!)
            )
            logger.debug("Preutfylling Utfylling Pensjon gjenlevende END")

            return pensjon
        }
        logger.debug("SED er ikke P6000")
        val pensjonfake = Pensjon()
//            gjenlevende = Bruker(
//                person = Person(
//                    fornavn = "F",
//                    kjoenn = "k",
//                    foedselsdato = "1901-12-01",
//                    etternavn = "E"
//                )
//            )
//        )
        return pensjonfake
    }

}