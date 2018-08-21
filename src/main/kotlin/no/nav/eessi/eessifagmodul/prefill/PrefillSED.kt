package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.Krav
import no.nav.eessi.eessifagmodul.models.SED
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Component
class PrefillSED(private val prefillPerson: PrefillPerson): Prefill<PrefillDataModel> {

    override fun prefill(prefillData: PrefillDataModel): PrefillDataModel {

        return when (prefillData.getSEDid())  {
            "P2000" -> {
                prefillPerson.prefill (prefillData)
                //sed.pensjon = null
                prefillData
            }
            "P6000" -> {
                prefillPerson.prefill(prefillData)
                //sed
                prefillData
            }
            "P4000" -> {
                //skal person data komme fra P4000? eller kun fra TPS?
                val sed = prefillPerson.prefill(prefillData)
                sed.trygdetid = PrefillP4000().prefill(prefillData)
                //sed
                prefillData
            }
            "P5000" -> {
                prefillPerson.prefill(prefillData)
                //sed
                prefillData
            }
            else -> throw IllegalArgumentException("Mangler SED, eller ugyldig type SED")
        }
    }


}