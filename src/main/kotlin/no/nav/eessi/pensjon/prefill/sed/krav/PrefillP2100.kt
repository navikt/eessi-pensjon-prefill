package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.pensjonsinformasjon.KravHistorikkHelper
import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype.ALDER
import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype.UFOREP
import no.nav.eessi.pensjon.pensjonsinformasjon.models.PenKravtype.FORSTEG_BH
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.pensjon.v1.sak.V1Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class PrefillP2100(private val prefillNav: PrefillPDLNav) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP2100::class.java) }

    fun prefillSed(prefillData: PrefillDataModel, personData: PersonDataCollection, sak: V1Sak?): Pair<String?, SED> {
        val pensjon = PrefillP2xxxPensjon.populerPensjon(prefillData, sak)

        //TPS
        postLog(prefillData, sak)
        val nav = prefillNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            avdod = prefillData.avdod,
            personData = personData,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
            krav = pensjon?.kravDato,
            annenPerson = null
        )
        val gjenlevende = prefillData.avdod?.let { prefillNav.createGjenlevende(personData.forsikretPerson) }

        return prefillPen(prefillData, nav, gjenlevende, sak)
    }

    private fun postLog(prefillData: PrefillDataModel, sak: V1Sak?) {
        require(prefillData.avdod != null ) { "avdod er påkrevet for p2100" }
        logger.debug("\n\n----------------------------------------------------------"
                + "\nSaktype                : ${sak?.sakType} "
                + "\nSøker sakId            : ${prefillData.penSaksnummer} "
                + "\nKravdato, kravtype     : ${prefillData.kravDato}, ${prefillData.kravType} "
                + "\nSøker avdodaktor       : ${prefillData.avdod.aktorId} "
                + "\nerGyldigEtterlatt      : ${prefillData.avdod.aktorId.isNotEmpty()} "
                + "\nSøker gjenlevaktoer    : ${prefillData.bruker.aktorId} "
                + "\n------------------| Preutfylling [${prefillData.sedType}] START |------------------ \n")
    }


    private fun prefillPen(prefillData: PrefillDataModel, nav: Nav, gjenlev: Bruker? = null, sak: V1Sak?): Pair<String?, SED> {

        val andreInstitusjondetaljer = EessiInformasjon().asAndreinstitusjonerItem()

        validerGyldigKravtypeOgArsak(sak, prefillData.sedType)
        var melding: String? = ""
        var pensjon = Pensjon()
        try {
                val meldingOmPensjon = PrefillP2xxxPensjon.populerMeldinOmPensjon(
                        prefillData.bruker.norskIdent,
                        prefillData.penSaksnummer,
                        sak,
                        andreInstitusjondetaljer,
                        gjenlev,
                    prefillData.kravId)
                melding = meldingOmPensjon.melding
                pensjon = Pensjon(
                        kravDato =  meldingOmPensjon.pensjon.kravDato ?: prefillData.kravDato?.let { Krav(it, prefillData.kravType?.verdi) },
                        gjenlevende = meldingOmPensjon.pensjon.gjenlevende
                ) //vi skal ha blank pensjon, men vi må ha med kravdato
        } catch (ex: Exception) {
            logger.error(ex.message, ex)
        }

        val sed = P2100(
            SedType.P2100,
            nav = nav,
            pensjon = pensjon
        )

        logger.debug("-------------------| Preutfylling [$SedType] END |------------------- ")
        return Pair(melding, sed)
    }

    /**
     * Skal validere på kravtype og kravårrsak Krav SED P2100 Gjenlev
     * https://confluence.adeo.no/pages/viewpage.action?pageId=338181302
     *
     * FORSTEG_BH       Førstegangsbehandling (ingen andre) skal vi avslutte
     * F_BH_KUN_UTL     Førstegangsbehandling utland (ingen andre) skal vi avslutte
     *
     * Kravårsak:
     * GJNL_SKAL_VURD  Gjenlevendetillegg skal vurderes     hvis ikke finnes ved P2100 skal vi avslutte
     * TILST_DOD       Dødsfall tilstøtende                 hvis ikke finnes ved
     *
     */
    private fun validerGyldigKravtypeOgArsak(sak: V1Sak?, sedType: SedType) {
        logger.info("Start på validering av $sedType")

        PrefillP2xxxPensjon.avsluttHvisKunDenneKravTypeIHistorikk(sak, sedType, FORSTEG_BH)

        if (KravHistorikkHelper.hentKravhistorikkForGjenlevende(sak?.kravHistorikkListe) == null
                    && listOf(ALDER.name, UFOREP.name).contains(sak?.sakType)  ) {
            logger.warn("Ikke korrekt kravårsak for P2100 (alder/uførep")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingen gyldig kravårsak funnet for ALDER eller UFØREP for utfylling av en krav SED P2100")
        }
        logger.info("Avslutter på validering av $sedType, fortsetter med preutfylling")
    }

}
