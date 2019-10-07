package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.fagmodul.sedmodel.Nav
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.model.ValidationException
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2xxxPensjon.addRelasjonerBarnOgAvdod
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2xxxPensjon.createPensjon
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillPersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.eessi.pensjon.fagmodul.sedmodel.Pensjon
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjoninformasjonException
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * preutfylling av NAV-P2000 SED for søknad krav om alderpensjon
 */
class PrefillP2000(private val prefillNav: PrefillNav,
                   private val dataFromPEN: PensjonsinformasjonHjelper,
                   private val preutfyllingPersonFraTPS: PrefillPersonDataFromTPS) : Prefill<SED> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP2000::class.java) }


    override fun prefill(prefillData: PrefillDataModel): SED {
        val sedId = prefillData.getSEDid()

        prefillData.saktype = Saktype.ALDER.name
        logger.debug("----------------------------------------------------------"
                + "\nSaktype              : ${prefillData.saktype} "
                + "\nSøker etter SaktId   : ${prefillData.penSaksnummer} "
                + "\nPreutfylling Pensjon : ${PrefillP2xxxPensjon::class.java} "
                + "\n------------------| Preutfylling [$sedId] START |------------------ ")

        val sed = prefillData.sed

        //skipper å hente persondata dersom NAVSED finnes
        if (prefillData.kanFeltSkippes("NAVSED")) {
            sed.nav = Nav()
        } else {
            //henter opp persondata
            sed.nav = prefillNav.prefill(prefillData, fyllUtBarnListe = true)
        }

        try {
            val pendata: Pensjonsinformasjon? = hentPensjonsdata(prefillData.aktoerID)
            if (pendata != null) addRelasjonerBarnOgAvdod(prefillData, pendata)
            sed.pensjon =
                if (pendata == null) Pensjon()
                else {
                    val pensjon = createPensjon(
                            prefillData.personNr,
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
        return if (!prefillData.kanFeltSkippes("PENSED") && prefillData.erGyldigEtterlatt()) {
            logger.debug("          Utfylling gjenlevende (etterlatt)")
            val gjenlevendeBruker = preutfyllingPersonFraTPS.hentBrukerFraTPS(prefillData.personNr)
            if (gjenlevendeBruker == null) null else prefillNav.createBruker(gjenlevendeBruker, null, null)
        } else null
    }

    fun hentPensjonsdata(aktoerId: String) =
            try {
                dataFromPEN.hentPersonInformasjonMedAktoerId(aktoerId)
            } catch (pen: PensjoninformasjonException) {
                logger.error(pen.message)
                null
            }


    override fun validate(data: SED) {
        when {
            data.nav?.bruker?.person?.etternavn == null -> throw ValidationException("Etternavn mangler")
            data.nav?.bruker?.person?.fornavn == null -> throw ValidationException("Fornavn mangler")
            data.nav?.bruker?.person?.foedselsdato == null -> throw ValidationException("Fødseldsdato mangler")
            data.nav?.bruker?.person?.kjoenn == null -> throw ValidationException("Kjønn mangler")
            data.nav?.krav?.dato == null -> throw ValidationException("Kravdato mangler")
        }
    }
}
