package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillSED
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2xxxPensjon.createPensjon
import no.nav.eessi.pensjon.fagmodul.prefill.tps.BrukerFromTPS
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.eessi.pensjon.fagmodul.sedmodel.Nav
import no.nav.eessi.pensjon.fagmodul.sedmodel.Pensjon
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjoninformasjonException
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * preutfylling av NAV-P2000 SED for søknad krav om alderpensjon
 */
class PrefillP2000(private val prefillNav: PrefillNav,
                   private val dataFromPEN: PensjonsinformasjonHjelper,
                   private val brukerFromTPS: BrukerFromTPS) : Prefill<SED> {

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
        validate(prefillData)
        return prefillData.sed
    }

    private fun eventuellGjenlevende(prefillData: PrefillDataModel): Bruker? {
        return if (!prefillData.kanFeltSkippes("PENSED") && prefillData.avdod != null) {
            logger.debug("          Utfylling gjenlevende (etterlatt)")
            val gjenlevendeBruker = brukerFromTPS.hentBrukerFraTPS(prefillData.bruker.norskIdent)
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

    private fun validate(data: PrefillDataModel) {
        when {
            data.sed.nav?.bruker?.person?.etternavn == null -> throw ValidationException("Etternavn mangler")
            data.sed.nav?.bruker?.person?.fornavn == null -> throw ValidationException("Fornavn mangler")
            data.sed.nav?.bruker?.person?.foedselsdato == null -> throw ValidationException("Fødseldsdato mangler")
            data.sed.nav?.bruker?.person?.kjoenn == null -> throw ValidationException("Kjønn mangler")
            data.sed.nav?.krav?.dato == null -> throw ValidationException("Kravdato mangler")
        }
    }
}

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
class ValidationException(message: String) : IllegalArgumentException(message)