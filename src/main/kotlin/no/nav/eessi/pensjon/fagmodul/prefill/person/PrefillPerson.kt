package no.nav.eessi.pensjon.fagmodul.prefill.person

import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.sedmodel.Pensjon
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjoninformasjonException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
//TODO: Denne klasser vil nok utgå når alle SED er klar med egen Preutfylling..
class PrefillPerson(private val prefillNav: PrefillNav, private val prefilliPensjon: PrefillPensjon) : Prefill<SED> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPerson::class.java) }

    override fun prefill(prefillData: PrefillDataModel): SED {

        logger.debug("----------------------------------------------------------")

        logger.debug("Preutfylling NAV     : ${prefillNav::class.java} ")
        logger.debug("Preutfylling Pensjon : ${prefilliPensjon::class.java} ")

        logger.debug("------------------| Preutfylling START |------------------ ")

        logger.debug("[${prefillData.getSEDid()}] Preutfylling Utfylling Data")

        val sed = prefillData.sed

        if (prefillData.kanFeltSkippes("NAVSED")) {
            //skipper å hente persondata dersom NAVSED finnes
            sed.nav = null
        } else {
            //henter opp persondata
            sed.nav = prefillNav.prefill(prefillData)
        }
        logger.debug("[${prefillData.getSEDid()}] Preutfylling Utfylling NAV")

        try {
            //henter opp pensjondata (her kun gjennlevende)
            sed.pensjon = prefilliPensjon.prefill(prefillData)
            logger.debug("[${prefillData.getSEDid()}] Preutfylling Utfylling Pensjon")
        } catch (pen: PensjoninformasjonException) {
            logger.error(pen.message)
            sed.pensjon = Pensjon()
        } catch (ex: Exception) {
            logger.error(ex.message, ex)
        }

        //Spesielle SED som har etterlette men benyttes av flere BUC
        //Må legge gjenlevende også som nav.annenperson
        if (prefillData.avdod != null) {
            sed.nav?.annenperson = sed.pensjon?.gjenlevende
            sed.nav?.annenperson?.person?.rolle = "01"  //Claimant
        }

        logger.debug("-------------------| Preutfylling END |------------------- ")
        return prefillData.sed

    }

}

