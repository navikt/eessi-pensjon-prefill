package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonData
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.eessi.pensjon.fagmodul.sedmodel.Kontekst
import no.nav.eessi.pensjon.fagmodul.sedmodel.Leggtilinstitusjon
import no.nav.eessi.pensjon.fagmodul.sedmodel.Nav
import no.nav.eessi.pensjon.fagmodul.sedmodel.Navsak
import no.nav.eessi.pensjon.fagmodul.sedmodel.Person
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillX005(private val prefillNav: PrefillNav)  {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillX005::class.java) }

    fun prefill(prefillData: PrefillDataModel, personData: PersonData): SED {

        val navsed = prefillNav.prefill(
                penSaksnummer = prefillData.penSaksnummer,
                bruker = prefillData.bruker,
                avdod = prefillData.avdod,
                personData = personData,
                brukerInformasjon = prefillData.getPersonInfoFromRequestData()
        )

        logger.debug("Tilpasser X005 forenklet preutfylling")
        val person = navsed.bruker?.person

        return SED(
                sed = "X005",
                nav = Nav(
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
                                leggtilinstitusjon = Leggtilinstitusjon(
                                        institusjon = prefillData.institusjonX005,
                                        grunn = null
                                )
                        )
                )
        ).also {
            logger.debug("Tilpasser X005 forenklet preutfylling, Ferdig.")
        }
    }

}
