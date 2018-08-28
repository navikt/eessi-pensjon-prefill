package no.nav.eessi.eessifagmodul.prefill

import org.springframework.stereotype.Component

@Component
class PrefillSED(private val prefillPerson: PrefillPerson) : Prefill<PrefillDataModel> {

    override fun prefill(prefillData: PrefillDataModel): PrefillDataModel {

        return when (prefillData.getSEDid()) {
            "P2000" -> {
                prefillPerson.prefill(prefillData)
                prefillData
            }
            "P6000" -> {
                prefillPerson.prefill(prefillData)
                prefillData
            }
            "P4000" -> {
                //skal person data komme fra P4000? eller kun fra TPS?
                val sed = prefillPerson.prefill(prefillData)
                sed.trygdetid = PrefillP4000().prefill(prefillData)
                prefillData
            }
            "P5000" -> {
                prefillPerson.prefill(prefillData)
                prefillData
            }
            else -> throw IllegalArgumentException("Mangler SED, eller ugyldig type SED")
        }
    }
}