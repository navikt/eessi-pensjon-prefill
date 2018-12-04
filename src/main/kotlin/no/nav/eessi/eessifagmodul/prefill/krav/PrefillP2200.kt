package no.nav.eessi.eessifagmodul.prefill.krav

import no.nav.eessi.eessifagmodul.models.Bruker
import no.nav.eessi.eessifagmodul.models.Nav
import no.nav.eessi.eessifagmodul.models.Pensjon
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.prefill.PensjonsinformasjonHjelper
import no.nav.eessi.eessifagmodul.prefill.Prefill
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.prefill.nav.PrefillNav
import no.nav.eessi.eessifagmodul.prefill.nav.PrefillPersonDataFromTPS
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * preutfylling av NAV-P2200 SED for søknad krav om uforepensjon
 */
class PrefillP2200(private val prefillNav: PrefillNav, private val preutfyllingPersonFraTPS: PrefillPersonDataFromTPS, pensjonHjelper: PensjonsinformasjonHjelper) : Prefill<SED> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP2200::class.java) }

    private val sakPensiondata: KravDataFromPEN = KravDataFromPEN(pensjonHjelper)

    override fun prefill(prefillData: PrefillDataModel): SED {
        val sedId = prefillData.getSEDid()

        logger.debug("----------------------------------------------------------"
                + "\nPreutfylling NAV     : ${prefillNav::class.java} "
                + "\nPreutfylling TPS     : ${preutfyllingPersonFraTPS::class.java} "
                + "\nPreutfylling Pensjon : ${sakPensiondata::class.java} "
                + "\n------------------| Preutfylling [$sedId] START |------------------ ")

        val sed = prefillData.sed

        logger.debug("Henter opp Pernsjondata fra PESYS")
        val pensjon = createPensjon(prefillData)
        //legger til pensjon på SED
        sed.pensjon = pensjon

        logger.debug("Henter opp Persondata fra TPS")
        sed.nav = createNav(prefillData)


        logger.debug("Legger til 4. Informasjon om ytelser den forsikrede mottar")

        //TODO: 5. Ektefelle hva kan vi hente av informasjon? fra hvor
        logger.debug("Legger til 5. Ektefelle")
        logger.debug("      mangler familieforhold fra EP-Selvbetjening.")

        //TODO: 7. Informasjon om representant/verge hva kan vi hente av informasjon? fra hvor
        logger.debug("Legger til 7. Informasjon om representant/verge")

        logger.debug("Henter opp Persondata/Gjenlevende fra TPS")
        pensjon.gjenlevende = createGjenlevende(prefillData)

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
        return sakPensiondata.prefill(prefillData)
    }

    //fylles ut kun når vi har etterlatt etterlattPinID.
    //noe vi må få fra PSAK. o.l
    private fun createGjenlevende(prefillData: PrefillDataModel): Bruker? {
        var gjenlevende: Bruker? = null
        if (prefillData.erGyldigEtterlatt()) {
            logger.debug("          Utfylling gjenlevende (etterlatt)")
            gjenlevende = preutfyllingPersonFraTPS.prefillBruker(prefillData.personNr)
        }
        return gjenlevende
    }

}