package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.prefill.nav.PrefillPerson

class PrefillP2100(private val prefillPerson: PrefillPerson) : Prefill<SED> {

    override fun prefill(prefillData: PrefillDataModel): SED {

        return prefillPerson.prefill(prefillData)
    }

}