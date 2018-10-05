package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.utils.STANDARD_SED
import no.nav.eessi.eessifagmodul.utils.SedEnum
import no.nav.eessi.eessifagmodul.utils.validsed
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
/**
 * Deligate sed to dedicated prefillclass
 */
class PrefillSED(private val factory: PrefillFactory) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillSED::class.java) }

    fun prefill(prefillData: PrefillDataModel): PrefillDataModel {

        val prefilling = factory.createPrefillClass(prefillData)
        logger.debug("Mapping prefillClass ${prefilling.javaClass}")

        val starttime = System.currentTimeMillis()
        logger.debug(" henter pensjon data fra PESYS ")
        val seddata = prefilling.prefill(prefillData)
        val endtime = System.currentTimeMillis()
        val tottime = endtime - starttime
        logger.debug(" ferdig PrefillSED. Det tok $tottime ms")
        println(seddata)
        return prefillData

    }
}