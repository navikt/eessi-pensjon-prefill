package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.Bruker
import no.nav.eessi.eessifagmodul.models.SED
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PrefillP6000(private val prefillNav: PrefillNav, private val prefillPensjonFromPen: PrefillPensionDataFromPESYS, private val preutfyllingPersonFraTPS: PrefillPersonDataFromTPS): Prefill<SED> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP6000::class.java) }

    override fun prefill(prefillData: PrefillDataModel): SED {

        val sedId = prefillData.getSEDid()

        logger.debug("----------------------------------------------------------")

        logger.debug("Preutfylling NAV     : ${prefillNav::class.java} ")
        logger.debug("Preutfylling TPS     : ${preutfyllingPersonFraTPS::class.java} ")
        logger.debug("Preutfylling Pensjon : ${prefillPensjonFromPen::class.java} ")

        logger.debug("------------------| Preutfylling [$sedId] START |------------------ ")

        logger.debug("[$$sedId] Preutfylling Utfylling Data")

        val sed = prefillData.sed

        sed.nav = prefillNav.utfyllNav(prefillData)

        logger.debug("[$sedId] Preutfylling Utfylling NAV")

        //henter pensjondata fra PESYS
        val pensjon = prefillPensjonFromPen.prefill(prefillData)
        logger.debug("[$sedId] Preutfylling Utfylling Pensjon")

        //fylle på med annen persondata fra TPS o.l
        pensjon.gjenlevende = createGjenlevende(prefillData)
        logger.debug("[$sedId] Preutfylling Utfylling Pensjon TPS")


        sed.pensjon = pensjon

        logger.debug("-------------------| Preutfylling [$sedId] END |------------------- ")

        return prefillData.sed

    }

    //fylles ut kun når vi har etterlatt aktoerId og etterlattPinID.
    //noe vi må få fra PSAK. o.l

    private fun createGjenlevende(prefillData: PrefillDataModel): Bruker? {
        var gjenlevende: Bruker? = null
        if (prefillData.isValidEtterlatt()) {
            logger.debug("Preutfylling Utfylling Pensjon Gjenlevende (etterlatt)")
            gjenlevende = preutfyllingPersonFraTPS.prefillBruker(prefillData.personNr)
        }
        return gjenlevende
    }



}

