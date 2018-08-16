package no.nav.eessi.eessifagmodul.services

import no.nav.eessi.eessifagmodul.models.RINAaksjoner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EuxMuligeAksjoner(private val euxService: EuxService) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EuxMuligeAksjoner::class.java) }

    private val timeTries = 5               // times to try
    private val waittime : Long = 1200      // waittime (basis venter 6000 på flere tjenester?)

    private val create = "Create"
    private val update = "Update"

    fun confirmUpdate(sed: String, rinanr: String) : Boolean {
        return kanOppdatere(sed, rinanr, update, 1)
    }
    fun confirmCreate(sed: String, rinanr: String) : Boolean {
        return kanOppdatere(sed, rinanr, create,  5)
    }

    private fun kanOppdatere(sed: String, rinanr: String, navn: String, deep: Int = 1) : Boolean {
        logger.debug("henter RINAaksjoner på sed: $sed, mot rinanr: $rinanr, letter etter: $navn og deep er: $deep")

        var validCheck : Boolean = false
        val result = getMuligeAksjoner(rinanr)

        run breaker@ {
            result.forEach {
                logger.debug("iterating igjennon aksjoner for å finne $sed. har: ${it.dokumentType} og ${it.navn} ")
                if (sed == it.dokumentType && navn == it.navn) {
                    validCheck = true
                    logger.debug("Funnet sed og sjekker om '$navn' finnes. validCheck: $validCheck")
                    return@breaker //exit foreatch
                }
            }
        }
        logger.debug("Slutt paa resultatlist validCheck: $validCheck")

        if (validCheck) {
            logger.debug("alt funnet rett verdi")
            return validCheck
        } else if (deep >= timeTries) {
            logger.debug("antall tries er kjørt")
            return validCheck
        }
        logger.debug("prøver igjen etter $waittime ms på å hente opp aksjoner...")
        Thread.sleep(waittime)
        return kanOppdatere(sed, rinanr, navn, deep+1)
    }

    private fun getMuligeAksjoner(rinanr: String): List<RINAaksjoner> {
        return euxService.getMuligeAksjoner(rinanr)
    }


}