package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.models.SED
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillPerson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillDefaultSED(private val prefillPerson: PrefillPerson) : Prefill<SED> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillDefaultSED::class.java) }

    override fun prefill(prefillData: PrefillDataModel): SED {

        logger.info("Default SED prefill [${prefillData.getSEDid()}]")

        prefillPerson.prefill(prefillData)
        return prefillData.sed
    }

}