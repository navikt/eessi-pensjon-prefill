package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.H02x
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.Person
import no.nav.eessi.pensjon.eux.model.sed.PinLandItem
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.models.person.PrefillPDLNav

class PrefillH02X(private val prefillNav: PrefillPDLNav)  {

    fun prefill(prefillData: PrefillDataModel, personData: PersonDataCollection): H02x {

        val navSed = prefillNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            avdod = prefillData.avdod,
            personData = personData,
            brukerInformasjon = prefillData.getPersonInfoFromRequestData(),
            annenPerson = null
        )

        val pinitem = navSed.bruker?.person?.pin?.first()

        val pinLand = PinLandItem(
                oppholdsland = pinitem?.land,
                kompetenteuland = pinitem?.identifikator
        )

        return H02x(
            type = prefillData.sedType,
            nav = Nav(
                bruker = Bruker(
                    person = Person(
                        pinland = pinLand
                    )
                )
            )
        )

    }
}