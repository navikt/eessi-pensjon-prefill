package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.SED
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PrefillPerson(private val prefillNav: PrefillNav, private val prefilliPensjon: PrefillPensjon) : Prefill<SED> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPerson::class.java) }

    override fun prefill(prefillData: PrefillDataModel): SED {

        logger.debug("----------------------------------------------------------")

        logger.debug("Preutfylling NAV     : ${prefillNav::class.java} ")
        logger.debug("Preutfylling Pensjon : ${prefilliPensjon::class.java} ")

        logger.debug("------------------| Preutfylling START |------------------ ")

        logger.debug("[${prefillData.getSEDid()}] Preutfylling Utfylling Data")

        val sed = prefillData.sed

        sed.nav = prefillNav.prefill(prefillData)

        logger.debug("[${prefillData.getSEDid()}] Preutfylling Utfylling NAV")

        sed.pensjon = prefilliPensjon.prefill(prefillData)

        logger.debug("[${prefillData.getSEDid()}] Preutfylling Utfylling Pensjon")

        logger.debug("-------------------| Preutfylling END |------------------- ")
        return prefillData.sed

    }

}

