package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonData
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.PrefillP6000Pensjon.createPensjon
import no.nav.eessi.pensjon.fagmodul.prefill.tps.TpsPersonService
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP6000(private val prefillNav: PrefillNav,
                   private val eessiInfo: EessiInformasjon,
                   private val dataFromPESYS: PensjonsinformasjonService,
                   private val tpsPersonService: TpsPersonService) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP6000::class.java) }

    fun prefill(prefillData: PrefillDataModel, personData: PersonData): SED {
        val sedId = prefillData.getSEDid()

        logger.debug("----------------------------------------------------------"
                + "\nPreutfylling Pensjon : ${PrefillP6000Pensjon::class.java} "
                + "\n------------------| Preutfylling [$sedId] START |------------------ ")

        val sed = prefillData.sed

        logger.info("Henter ut lokal kontakt, institusjon (NAV Utland)")
        prefillData.andreInstitusjon = eessiInfo.asAndreinstitusjonerItem()
        logger.info("Andreinstitusjoner: ${prefillData.andreInstitusjon} ")

        logger.debug("Henter opp Persondata/Gjenlevende fra TPS")

        val gjenlevende = if (prefillData.avdod != null) {
            val gjenlevendeBruker = tpsPersonService.hentBrukerFraTPS(prefillData.bruker.norskIdent)
            if (gjenlevendeBruker == null) null else prefillNav.createBruker(gjenlevendeBruker, null, null)
        } else null

        logger.debug("Henter opp Pernsjondata fra PESYS")
        sed.pensjon = createPensjon(dataFromPESYS, gjenlevende, prefillData.vedtakId, prefillData.andreInstitusjon)

        logger.debug("Henter opp Persondata fra TPS")
        sed.nav = prefillNav.prefill(penSaksnummer = prefillData.penSaksnummer, bruker = prefillData.bruker, avdod = prefillData.avdod, personData = personData, brukerInformasjon = prefillData.getPersonInfoFromRequestData())

        logger.debug("-------------------| Preutfylling [$sedId] END |------------------- ")
        return prefillData.sed
    }
}

