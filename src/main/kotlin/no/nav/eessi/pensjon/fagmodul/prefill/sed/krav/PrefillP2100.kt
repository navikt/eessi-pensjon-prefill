package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.tps.BrukerFromTPS
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.eessi.pensjon.fagmodul.sedmodel.Nav
import no.nav.eessi.pensjon.fagmodul.sedmodel.Pensjon
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjoninformasjonException
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP2100(private val prefillNav: PrefillNav,
                   private val dataFromPEN: PensjonsinformasjonHjelper,
                   private val brukerFromTPS: BrukerFromTPS) : Prefill<SED> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP2100::class.java) }


    override fun prefill(prefillData: PrefillDataModel): SED {
        val sedId = prefillData.getSEDid()

        prefillData.saktype = Saktype.GJENLEV.name
        logger.debug("\n\n----------------------------------------------------------"
                + "\nSaktype                  : ${prefillData.saktype} "
                + "\nSøker etter SaktId       : ${prefillData.penSaksnummer} "
                + "\nSøker etter avdodaktor   : ${prefillData.avdod?.aktorId} "
                + "\nerGyldigEtterlatt        : ${prefillData.avdod?.aktorId?.isNotEmpty()} "
                + "\nSøker etter gjenlaktoer  : ${prefillData.bruker.aktorId} "
                + "\n------------------| Preutfylling [$sedId] START |------------------ \n")

        val sed = prefillData.sed

        //skipper å hente persondata dersom NAVSED finnes
        if (prefillData.kanFeltSkippes("NAVSED")) {
            sed.nav = Nav()
        } else {
            //henter opp persondata
            sed.nav = prefillNav.prefill(penSaksnummer = prefillData.penSaksnummer, bruker = prefillData.bruker, avdod = prefillData.avdod, fyllUtBarnListe = true, brukerInformasjon = prefillData.getPersonInfoFromRequestData())
        }

        try {
            val evtgjennlevende = eventuellGjenlevende(prefillData)
            val pendata: Pensjonsinformasjon? = hentPensjonsdata(prefillData.bruker.aktorId)
            sed.pensjon =
                    if (pendata == null) Pensjon()
                    else {
                        val pensjon = PrefillP2xxxPensjon.createPensjon(
                                prefillData.bruker.norskIdent,
                                prefillData.penSaksnummer,
                                evtgjennlevende,
                                pendata,
                                prefillData.andreInstitusjon)
                        if (prefillData.kanFeltSkippes("PENSED")) {
                            Pensjon(
                                    kravDato = pensjon.kravDato,
                                    gjenlevende = pensjon.gjenlevende
                            ) //vi skal ha blank pensjon ved denne toggle, men vi må ha med kravdato
                        } else {
                            pensjon
                        }
                    }
        } catch (ex: Exception) {
            logger.error(ex.message, ex)
            // TODO Should we really swallow this?
            // TODO What's consequences by removing this? and throw it up to UI?
        }

        KravHistorikkHelper.settKravdato(prefillData, sed)

        logger.debug("-------------------| Preutfylling [$sedId] END |------------------- ")
        return prefillData.sed
    }

    private fun eventuellGjenlevende(prefillData: PrefillDataModel): Bruker? {
        return if (prefillData.avdod != null) {
            logger.debug("          Utfylling gjenlevende (etterlatt)")
            val gjenlevendeBruker = brukerFromTPS.hentBrukerFraTPS(prefillData.bruker.norskIdent)
            return if (gjenlevendeBruker == null) null else prefillNav.createBruker(gjenlevendeBruker, null, null)
        } else null
    }

    fun hentPensjonsdata(aktoerId: String) =
            try {
                dataFromPEN.hentPersonInformasjonMedAktoerId(aktoerId)
            } catch (pen: PensjoninformasjonException) {
                logger.error(pen.message)
                null
            }

}
