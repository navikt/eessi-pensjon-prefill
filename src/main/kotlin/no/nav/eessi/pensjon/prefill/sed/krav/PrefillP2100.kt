package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.P2100
import no.nav.eessi.pensjon.eux.model.sed.Pensjon
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.pensjon.v1.sak.V1Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
            bankOgArbeid = prefillData.getPersonInfoFromRequestData(),
            krav = pensjon?.kravDato,
            annenPerson = null
        )
        val gjenlev = prefillNav.eventuellGjenlevendePDL(prefillData.avdod, personData.forsikretPerson)

        return prefillPen(prefillData, nav, gjenlev, sak)
    }

    private fun postLog(prefillData: PrefillDataModel, sak: V1Sak?) {
        require(prefillData.avdod != null ) { "avdod er påkrevet for p2100" }
        logger.debug("\n\n----------------------------------------------------------"
                + "\nSaktype                : ${sak?.sakType} "
                + "\nSøker sakId            : ${prefillData.penSaksnummer} "
                + "\nSøker avdodaktor       : ${prefillData.avdod.aktorId} "
                + "\nerGyldigEtterlatt      : ${prefillData.avdod.aktorId.isNotEmpty()} "
                + "\nSøker gjenlevaktoer    : ${prefillData.bruker.aktorId} "
                + "\n------------------| Preutfylling [${prefillData.sedType}] START |------------------ \n")
    }


    private fun prefillPen(prefillData: PrefillDataModel, nav: Nav, gjenlev: Bruker? = null, sak: V1Sak?): Pair<String?, SED> {
        val sedType = prefillData.sedType

        val andreInstitusjondetaljer = EessiInformasjon().asAndreinstitusjonerItem()

        PrefillP2xxxPensjon.validerGyldigKravtypeOgArsakGjenlevnde(sak, sedType)
        var melding: String? = ""
        var pensjon: Pensjon? = Pensjon()
        try {
                val meldingOmPensjon = PrefillP2xxxPensjon.populerMeldinOmPensjon(
                        prefillData.bruker.norskIdent,
                        prefillData.penSaksnummer,
                        sak,
                        andreInstitusjondetaljer,
                        gjenlev,
                    prefillData.kravId)
                melding = meldingOmPensjon.melding
                pensjon = meldingOmPensjon.pensjon
                if (prefillData.sedType != SedType.P6000) {
                    pensjon = Pensjon(
                            kravDato = meldingOmPensjon.pensjon.kravDato,
                            gjenlevende = meldingOmPensjon.pensjon.gjenlevende
                    ) //vi skal ha blank pensjon ved denne toggle, men vi må ha med kravdato
                }
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

}
