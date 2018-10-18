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
 * Deligate SED to dedicated prefillClass
 */
class PrefillSED(private val factory: PrefillFactory) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillSED::class.java) }

    fun prefill(prefillData: PrefillDataModel): PrefillDataModel {

        val prefilling = factory.createPrefillClass(prefillData)
        logger.debug("Mapping prefillClass ${prefilling.javaClass}")

        val starttime = System.currentTimeMillis()

        prefilling.prefill(prefillData)

        val endtime = System.currentTimeMillis()
        val tottime = endtime - starttime

        //Metrics..
        logger.debug("Ferdig med prefillClass, Det tok $tottime ms")
        return prefillData

    }
}