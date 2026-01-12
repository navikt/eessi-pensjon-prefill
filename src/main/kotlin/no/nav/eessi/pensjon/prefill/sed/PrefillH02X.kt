package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PrefillDataModel

class PrefillH02X(private val prefillNav: PrefillPDLNav)  {

    fun prefill(prefillData: PrefillDataModel, personData: PersonDataCollection): H02x {

        val navSed = prefillNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            personData = personData,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
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