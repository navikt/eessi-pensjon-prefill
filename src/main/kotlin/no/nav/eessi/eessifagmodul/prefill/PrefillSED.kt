package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.utils.P4000_SED
import no.nav.eessi.eessifagmodul.utils.STANDARD_SED
import no.nav.eessi.eessifagmodul.utils.validsed
import org.springframework.stereotype.Component

@Component
class PrefillSED(private val prefillPerson: PrefillPerson) : Prefill<PrefillDataModel> {

    override fun prefill(prefillData: PrefillDataModel): PrefillDataModel {

        return when {
            validsed(prefillData.getSEDid(), STANDARD_SED) -> {

            //"P2000","P2100","P2200","P6000","P5000" -> {
                prefillPerson.prefill(prefillData)
                prefillData
            }
            validsed(prefillData.getSEDid(), P4000_SED) -> {
                //skal person data komme fra P4000? eller kun fra TPS?
                val sed = prefillPerson.prefill(prefillData)
                sed.trygdetid = PrefillP4000().prefill(prefillData)
                prefillData
            }
            else -> throw IllegalArgumentException("Mangler SED, eller ugyldig type SED")
        }
    }
}