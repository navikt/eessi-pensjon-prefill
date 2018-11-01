package no.nav.eessi.eessifagmodul.prefill.vedtak

import no.nav.eessi.eessifagmodul.models.Bruker
import no.nav.eessi.eessifagmodul.models.Nav
import no.nav.eessi.eessifagmodul.models.Pensjon
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.prefill.Prefill
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.prefill.PrefillNav
import no.nav.eessi.eessifagmodul.prefill.PrefillPersonDataFromTPS
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PrefillP6000(private val prefillNav: PrefillNav, private val dataFromPEN: PensionDataFromPESYS, private val dataFromTPS: PrefillPersonDataFromTPS) : Prefill<SED> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP6000::class.java) }

    override fun prefill(prefillData: PrefillDataModel): SED {
        val sedId = prefillData.getSEDid()

        logger.debug("----------------------------------------------------------")

        logger.debug("Preutfylling NAV     : ${prefillNav::class.java} ")
        logger.debug("Preutfylling TPS     : ${dataFromTPS::class.java} ")
        logger.debug("Preutfylling Pensjon : ${dataFromPEN::class.java} ")

        logger.debug("------------------| Preutfylling [$sedId] START |------------------ ")

        logger.debug("[$$sedId] Preutfylling Utfylling Data")

        val sed = prefillData.sed

        logger.debug("Henter opp Pernsjondata fra PESYS")
        val pensjon = createPensjon(prefillData)

        logger.debug("Henter opp Persondata fra TPS")
        sed.nav = createNav(prefillData)

        pensjon.gjenlevende = createGjenlevende(prefillData)

        sed.pensjon = pensjon

        logger.debug("-------------------| Preutfylling [$sedId] END |------------------- ")

        return prefillData.sed
    }

    //henter persondata fra TPS fyller ut sed.nav
    private fun createNav(prefillData: PrefillDataModel): Nav {
        logger.debug("[${prefillData.getSEDid()}] Preutfylling Utfylling NAV")
        return prefillNav.prefill(prefillData)
    }

    //henter pensjondata fra PESYS fyller ut sed.pensjon
    private fun createPensjon(prefillData: PrefillDataModel): Pensjon {
        logger.debug("[${prefillData.getSEDid()}] Preutfylling Utfylling Pensjon")
        return dataFromPEN.prefill(prefillData)
    }

    //fylles ut kun når vi har etterlatt aktoerId og etterlattPinID.
    //noe vi må få fra PSAK. o.l
    private fun createGjenlevende(prefillData: PrefillDataModel): Bruker? {
        var gjenlevende: Bruker? = null
        if (prefillData.erGyldigEtterlatt()) {
            logger.debug("Preutfylling Utfylling Pensjon Gjenlevende (etterlatt)")
            gjenlevende = dataFromTPS.prefillBruker(prefillData.personNr)
        }
        logger.debug("[${prefillData.getSEDid()}] Preutfylling Utfylling Pensjon TPS")
        return gjenlevende
    }


}

