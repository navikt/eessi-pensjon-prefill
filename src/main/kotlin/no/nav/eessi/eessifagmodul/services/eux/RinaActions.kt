package no.nav.eessi.eessifagmodul.services.eux

import no.nav.eessi.eessifagmodul.models.RINAaksjoner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RinaActions(private val euxService: EuxService) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(RinaActions::class.java) }

    @Value("\${rinaaction.waittime:4000}")
    lateinit var waittime: String  // waittime (basis venter 6000 på flere tjenester?)

    var timeTries = 30               // times to try

    val create = "Create"
    val update = "Update"

    fun canUpdate(sed: String, rinanr: String): Boolean {
        return isActionPossible(sed, rinanr, update, 1)
    }

    fun canCreate(sed: String, rinanr: String): Boolean {
        return isActionPossible(sed, rinanr, create, 5)
    }

    fun isActionPossible(sed: String, rinanr: String, navn: String, deep: Int = 1): Boolean {
        logger.debug("Henter RINAaksjoner på sed: $sed, mot rinanr: $rinanr, leter etter: $navn og deep er: $deep")
        val result = getMuligeAksjoner(rinanr)

        result.forEach {
            if (sed == it.dokumentType && navn == it.navn) {
                return true
            }
        }
        if (deep >= timeTries) {
            return false
        }
        logger.debug("Prøver igjen etter $waittime ms på å hente opp aksjoner.")
        Thread.sleep(waittime.toLong())
        return isActionPossible(sed, rinanr, navn, deep + 1)
    }

    private fun getMuligeAksjoner(rinanr: String): List<RINAaksjoner> {
        return euxService.getPossibleActions(rinanr)
    }


}