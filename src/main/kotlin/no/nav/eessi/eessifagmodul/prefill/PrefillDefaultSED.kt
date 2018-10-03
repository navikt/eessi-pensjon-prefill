package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.SED
import org.springframework.stereotype.Component

@Component
class PrefillDefaultSED(private val prefillPerson: PrefillPerson): Prefill<SED> {

    override fun prefill(prefillData: PrefillDataModel): SED {

        prefillPerson.prefill(prefillData)
        return prefillData.sed
    }

}