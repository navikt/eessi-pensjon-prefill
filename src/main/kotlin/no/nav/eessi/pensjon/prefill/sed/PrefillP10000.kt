package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.sed.P10000
import no.nav.eessi.pensjon.prefill.models.BrukerInformasjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PersonId
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP10000(private val prefillNav: PrefillPDLNav) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP10000::class.java) }

    fun prefill(penSaksnummer: String,
                bruker: PersonId,
                avdod: PersonId?,
                brukerInformasjon: BrukerInformasjon?,
                personData: PersonDataCollection): P10000 {

        val gjenlevende = try {
            val gjenlevende = avdod?.let {
                logger.info("Preutfylling Utfylling Pensjon Gjenlevende (etterlatt)")
                prefillNav.createBruker(personData.forsikretPerson!!, null, null)
            }
            gjenlevende
        } catch (ex: Exception) {
            logger.error(ex.message, ex)
            null
        }

        //Spesielle SED som har etterlette men benyttes av flere BUC
        //Må legge gjenlevende også som nav.annenperson
        if (avdod != null) {
            gjenlevende?.person?.rolle = "01" //Claimant - etterlatte
        }

        //henter opp persondata
        val navSed = prefillNav.prefill(
            penSaksnummer = penSaksnummer,
            bruker = bruker,
            avdod = avdod,
            personData = personData,
            brukerInformasjon = brukerInformasjon,
            annenPerson = gjenlevende
        )

        logger.debug("-------------------| Preutfylling END |------------------- ")

        return P10000(nav = navSed)
    }
}