package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.BankOgArbeid
import no.nav.eessi.pensjon.shared.api.InstitusjonItem
import no.nav.eessi.pensjon.shared.api.PersonInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillX005(private val prefillNav: PrefillPDLNav)  {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillX005::class.java) }

    fun prefill(penSaksnummer: String?,
                bruker: PersonInfo,
                avdod: PersonInfo?,
                brukerinformasjon: BankOgArbeid?,
                institusjon: InstitusjonItem,
                personData: PersonDataCollection): X005 {

        logger.debug("Tilpasser X005 forenklet preutfylling")

        val navsed = prefillNav.prefill(
            penSaksnummer = penSaksnummer,
            bruker = bruker,
            personData = personData,
            bankOgArbeid = brukerinformasjon,
        )
        val gjenlevende = avdod?.let { prefillNav.createGjenlevende(personData.forsikretPerson, bruker) }

        val person =  gjenlevende?.person ?: navsed.bruker?.person

        val institusjonX005 = InstitusjonX005(
                   id = institusjon.checkAndConvertInstituion(),
                    navn = institusjon.name ?: institusjon.checkAndConvertInstituion()
        )

        return X005(
                type = SedType.X005,
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
                                leggtilinstitusjon = Leggtilinstitusjon(
                                        institusjon = institusjonX005
                                )
                        )
                )
        ).also {
            logger.debug("Tilpasser X005 forenklet preutfylling, Ferdig.")
        }
    }

}
