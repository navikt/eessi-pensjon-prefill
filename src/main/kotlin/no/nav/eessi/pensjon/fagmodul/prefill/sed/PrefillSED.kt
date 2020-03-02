package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
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
        logger.info("Mapping prefillClass: ${prefilling::class.java.simpleName}")

        //prefill person (tps) og pensjon (pesys) skjer her
        prefilling.prefill(prefillData)

        return prefillData
    }

    override fun validate(data: PrefillDataModel) {
        factory.createPrefillClass(data).validate(data.sed)
    }
}
