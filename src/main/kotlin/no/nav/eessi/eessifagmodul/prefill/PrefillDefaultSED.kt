package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.prefill.nav.PrefillPerson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

//@Component
class PrefillDefaultSED(private val prefillPerson: PrefillPerson) : Prefill<SED> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillDefaultSED::class.java) }

    override fun prefill(prefillData: PrefillDataModel): SED {

        logger.debug("Default SED prefill [${prefillData.getSEDid()}]")

        prefillPerson.prefill(prefillData)
        return prefillData.sed
    }

}