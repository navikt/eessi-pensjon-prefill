package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2xxxPensjon.createPensjon
import no.nav.eessi.pensjon.fagmodul.prefill.tps.BrukerFromTPS
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.eessi.pensjon.fagmodul.sedmodel.Pensjon
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjoninformasjonException
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * preutfylling av NAV-P2200 SED for søknad krav om uforepensjon
 */
class PrefillP2200(private val prefillNav: PrefillNav,
                   private val dataFromPEN: PensjonsinformasjonHjelper,
                   private val brukerFromTPS: BrukerFromTPS) : Prefill<SED> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP2200::class.java) }

    override fun prefill(prefillData: PrefillDataModel): SED {
        val sedId = prefillData.getSEDid()

        logger.debug("----------------------------------------------------------"
                + "\nPreutfylling Pensjon : ${PrefillP2xxxPensjon::class.java} "
                + "\n------------------| Preutfylling [$sedId] START |------------------ ")

        val sed = prefillData.sed

        //henter opp persondata
        sed.nav = prefillNav.prefill(prefillData, fyllUtBarnListe = true)

        //henter opp pensjondat
        try {
            val pendata: Pensjonsinformasjon? = hentPensjonsdata(prefillData.bruker.aktorId)

            sed.pensjon =
                    if (pendata == null) Pensjon()
                    else {
                        val pensjon = createPensjon(
                                prefillData.bruker.norskIdent,
                                prefillData.penSaksnummer,
                                eventuellGjenlevende(prefillData),
                                pendata,
                                prefillData.andreInstitusjon)
                        if (prefillData.kanFeltSkippes("PENSED")) {
                            Pensjon(kravDato = pensjon.kravDato) //vi skal ha blank pensjon ved denne toggle, men vi må ha med kravdato
                        } else {
                            pensjon
                        }
                    }
        } catch (ex: Exception) {
            logger.error(ex.message, ex)
            // TODO Should we really swallow this?
        }

        KravHistorikkHelper.settKravdato(prefillData, sed)

        logger.debug("-------------------| Preutfylling [$sedId] END |------------------- ")
        return prefillData.sed
    }

    private fun eventuellGjenlevende(prefillData: PrefillDataModel): Bruker? {
        return if (!prefillData.kanFeltSkippes("PENSED") && prefillData.avdod != null) {
            logger.debug("          Utfylling gjenlevende (etterlatt)")
            val gjenlevendeBruker = brukerFromTPS.hentBrukerFraTPS(prefillData.bruker.norskIdent)
            if (gjenlevendeBruker == null) null else prefillNav.createBruker(gjenlevendeBruker, null, null)
        } else null
    }

    fun hentPensjonsdata(aktoerId: String): Pensjonsinformasjon? =
            try {
                dataFromPEN.hentPersonInformasjonMedAktoerId(aktoerId)
            } catch (pen: PensjoninformasjonException) {
                logger.error(pen.message)
                null
            }

}
