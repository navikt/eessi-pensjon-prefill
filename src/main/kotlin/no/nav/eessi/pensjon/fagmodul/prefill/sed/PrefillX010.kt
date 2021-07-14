package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.Informasjon
import no.nav.eessi.pensjon.eux.model.sed.KommersenereItem
import no.nav.eessi.pensjon.eux.model.sed.Kontekst
import no.nav.eessi.pensjon.eux.model.sed.Navsak
import no.nav.eessi.pensjon.eux.model.sed.Paaminnelse
import no.nav.eessi.pensjon.eux.model.sed.Person
import no.nav.eessi.pensjon.eux.model.sed.Svar
import no.nav.eessi.pensjon.eux.model.sed.X010
import no.nav.eessi.pensjon.eux.model.sed.XNav
import no.nav.eessi.pensjon.fagmodul.models.BrukerInformasjon
import no.nav.eessi.pensjon.fagmodul.models.PersonDataCollection
import no.nav.eessi.pensjon.fagmodul.models.PersonId
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillPDLNav
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillX010(private val prefillNav: PrefillPDLNav)  {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillX010::class.java) }

    fun prefill(penSaksnummer: String,
                bruker: PersonId,
                avdod: PersonId?,
                brukerinformasjon: BrukerInformasjon?,
                personData: PersonDataCollection): X010 {

        val navsed = prefillNav.prefill(
            penSaksnummer = penSaksnummer,
            bruker = bruker,
            avdod = avdod,
            personData = personData,
            brukerInformasjon = brukerinformasjon,
        )

        logger.debug("Tilpasser X010 forenklet preutfylling")
        val person = navsed.bruker?.person

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
                                            kommersenere = listOf(
                                                KommersenereItem(
                                                    type = "dokument",
                                                    opplysninger = "."
                                            )
                                            )
                                        )
                                    )
                                )
                        )
                )
        ).also {
            logger.debug("Tilpasser X010 forenklet preutfylling, Ferdig.")
        }
    }

}
