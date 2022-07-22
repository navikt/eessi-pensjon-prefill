package no.nav.eessi.pensjon.prefill.person

import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.Pensjon
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillSed(private val prefillNav: PrefillPDLNav) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillSed::class.java) }

    fun prefill(prefillData: PrefillDataModel, personData: PersonDataCollection): SED {
        logger.debug("----------------------------------------------------------")
        logger.debug("Preutfylling NAV     : ${prefillNav::class.java} ")
        logger.debug("------------------| Preutfylling START |------------------ ")
        logger.debug("[${prefillData.sedType}] Preutfylling Utfylling Data")

        val sedType = prefillData.sedType

        val prefillPensjon = try {
            val pensjon = prefillData.avdod?.let {
                logger.info("Preutfylling Utfylling Pensjon Gjenlevende (etterlatt)")
                val gjenlevendePerson = prefillNav.createBruker(personData.forsikretPerson!!, null, null)
                Pensjon(gjenlevende = gjenlevendePerson)
            }
            logger.debug("[${prefillData.sedType}] Preutfylling Utfylling Pensjon")
            pensjon
        } catch (ex: Exception) {
            logger.error(ex.message, ex)
            Pensjon()
        }

        //henter opp persondata
        val navSed = prefillNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            avdod = prefillData.avdod,
            personData = personData,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
            krav = prefillPensjon?.kravDato,
            annenPerson = annenPersonHvisGjenlevende(prefillData, prefillPensjon?.gjenlevende)
        )
        logger.debug("[${prefillData.sedType}] Preutfylling Utfylling NAV")


        logger.debug("-------------------| Preutfylling END |------------------- ")
        return SED(sedType, nav = navSed, pensjon = prefillPensjon)

    }

    //Spesielle SED som har etterlette men benyttes av flere BUC
    //Må legge gjenlevende også som nav.annenperson
    private fun annenPersonHvisGjenlevende(prefillData: PrefillDataModel, gjenlevende: Bruker?): Bruker? {
        return if (prefillData.avdod != null) {
            gjenlevende?.person?.rolle = "01"  //Claimant - etterlatte
            gjenlevende
        } else null
    }

}

