package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP10000(private val prefillSed: PrefillSed) : Prefill {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP10000::class.java) }

    override fun prefill(prefillData: PrefillDataModel): SED {

        logger.info("Default SED prefill [${prefillData.getSEDid()}]")

        val sed = prefillSed.prefill(prefillData)
        if (prefillData.avdod != null) {
            logger.info("Preutfylling Utfylling Pensjon Gjenlevende (etterlatt)")
            sed.nav?.annenperson =  sed.pensjon?.gjenlevende
            sed.nav?.annenperson?.person?.rolle = "01"  //Claimant
            sed.pensjon = null
        }
        return sed
    }

}