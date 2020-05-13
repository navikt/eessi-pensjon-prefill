package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonData
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.fagmodul.sedmodel.PinLandItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED

class PrefillH02X(private val prefillSed: PrefillSed)  {

    fun prefill(prefillData: PrefillDataModel, personData: PersonData): SED {

        val personSed = prefillSed.prefill(
                prefillData = prefillData,
                personData = personData
        )

        val pinitem = personSed.nav?.bruker?.person?.pin?.first()

        val pinLand = PinLandItem(
                oppholdsland = pinitem?.land,
                kompetenteuland = pinitem?.identifikator
        )
        val sed = prefillData.sed
        sed.nav?.bruker?.person?.pinland = pinLand

        return prefillData.sed
    }
}