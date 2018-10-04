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

    val create = "Create"
    val update = "Update"

    fun canUpdate(sed: String, rinanr: String): Boolean {
        return isActionPossible(sed, rinanr, update, 1)
    }

    fun canCreate(sed: String, rinanr: String): Boolean {
        return isActionPossible(sed, rinanr, create, 5)
    }

    fun isActionPossible(sed: String, euxCaseId: String, keyWord: String, maxLoop: Int = 1): Boolean {
        for (i in 1..maxLoop) {
            logger.debug("Henter RINAaksjoner på sed: $sed, mot euxCaseId: $euxCaseId, leter etter: $keyWord og maxLoop er: $maxLoop og i er: $i")
            val result = getMuligeAksjoner(euxCaseId)
            result.forEach {
                if (sed == it.dokumentType && keyWord == it.navn) {
                    logger.debug("Found $keyWord for $sed  exit out.")
                    return true
                }
                logger.debug("Not found $keyWord for $sed")
            }
            logger.debug("Prøver igjen etter $waittime ms på å hente opp aksjoner.")
            Thread.sleep(waittime.toLong())
        }
        logger.debug("Max looping exit with false")
        return false
    }

    private fun getMuligeAksjoner(rinanr: String): List<RINAaksjoner> {
        return euxService.getPossibleActions(rinanr)
    }


}