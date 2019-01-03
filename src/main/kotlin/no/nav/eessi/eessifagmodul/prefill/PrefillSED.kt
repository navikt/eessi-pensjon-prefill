package no.nav.eessi.eessifagmodul.prefill

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
/**
 * Deligate SED to dedicated prefillClass
 */
class PrefillSED(private val factory: PrefillFactory) : Prefill<PrefillDataModel> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillSED::class.java) }

    override fun prefill(prefillData: PrefillDataModel): PrefillDataModel {

        val prefilling = factory.createPrefillClass(prefillData)
        logger.debug("Mapping prefillClass ${prefilling.javaClass}")

        val starttime = System.currentTimeMillis()

        //magic happens here...
        prefilling.prefill(prefillData)

        val endtime = System.currentTimeMillis()
        val tottime = endtime - starttime

        //Metrics..
        logger.debug("Ferdig med prefillClass, Det tok $tottime ms")

        return prefillData
    }

}
