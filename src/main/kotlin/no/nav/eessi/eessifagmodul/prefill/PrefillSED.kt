package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.SED
import org.springframework.stereotype.Service

@Service
class PrefillSED(private val prefillPerson: PrefillPerson) {

    fun prefill(prefillData: PrefillDataModel): PrefillDataModel {

        return when (prefillData.getSEDid())  {
            "P2000" -> {
                val sed = prefillPerson.prefill (prefillData)
                //sed.pensjon = null
                //sed
                prefillData
            }
            "P6000" -> {
                val sed = prefillPerson.prefill(prefillData)
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
                val sed = prefillPerson.prefill(prefillData)
                //sed
                prefillData
            }
            else -> throw IllegalArgumentException("Mangler SED, eller ugyldig type SED")
        }
    }


}