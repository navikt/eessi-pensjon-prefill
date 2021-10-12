package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.Informasjon
import no.nav.eessi.pensjon.eux.model.sed.KommersenereItem
import no.nav.eessi.pensjon.eux.model.sed.Kontekst
import no.nav.eessi.pensjon.eux.model.sed.Navsak
import no.nav.eessi.pensjon.eux.model.sed.Paaminnelse
import no.nav.eessi.pensjon.eux.model.sed.Person
import no.nav.eessi.pensjon.eux.model.sed.Svar
import no.nav.eessi.pensjon.eux.model.sed.X009
import no.nav.eessi.pensjon.eux.model.sed.X010
import no.nav.eessi.pensjon.eux.model.sed.XNav
import no.nav.eessi.pensjon.prefill.models.BrukerInformasjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PersonId
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillX010(private val prefillNav: PrefillPDLNav)  {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillX010::class.java) }

    fun prefill(penSaksnummer: String,
                bruker: PersonId,
                avdod: PersonId?,
                brukerinformasjon: BrukerInformasjon?,
                personData: PersonDataCollection,
                x009: X009? = null): X010 {

        logger.info("Tilpasser X010 preutfylling med data fra X009")

        val navsed = prefillNav.prefill(
            penSaksnummer = penSaksnummer,
            bruker = bruker,
            avdod = avdod,
            personData = personData,
            brukerInformasjon = brukerinformasjon,
        )
        val gjenlevende = avdod?.let {  prefillNav.eventuellGjenlevendePDL(it, personData.forsikretPerson) }
        val person =  gjenlevende?.person ?:  navsed.bruker?.person

        return X010 (
                xnav = XNav(
                        sak = Navsak(
                                kontekst = Kontekst(
                                        bruker = Bruker(
                                                person = Person(
                                                        fornavn = person?.fornavn,
                                                        etternavn = person?.etternavn,
                                                        foedselsdato = person?.foedselsdato,
                                                        kjoenn = person?.kjoenn
                                                )
                                        )
                                ),
                                paaminnelse = Paaminnelse(
                                    svar = Svar(
                                        informasjon = Informasjon(
                                            kommersenere = populerKommersenereFraX009(x009) ,
                                            ikketilgjengelig = null
                                        )
                                    )

                                )
                        )
                )
        ).also {
            logger.debug("Tilpasser X010 forenklet preutfylling, Ferdig.")
        }
    }

    private fun populerKommersenereFraX009(x009: X009?): List<KommersenereItem>? {
        return x009?.xnav?.sak?.paaminnelse?.sende?.mapNotNull { sendtItem ->
            KommersenereItem(type =  sendtItem?.type, opplysninger = sendtItem?.detaljer)
        }
    }

}
