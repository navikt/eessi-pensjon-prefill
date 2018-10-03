package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.utils.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PrefillSED: Prefill<PrefillDataModel> {

    @Autowired
    lateinit var prefill6000: PrefillP6000

    @Autowired
    lateinit var prefill2000: PrefillP2000

    @Autowired
    lateinit var prefill4000: PrefillP4000

    @Autowired
    lateinit var prefillDefault: PrefillDefaultSED

    override fun prefill(prefillData: PrefillDataModel): PrefillDataModel {

        val enumSED = ENUM_SED.valueOf(prefillData.getSEDid())

        return when(enumSED) {
            ENUM_SED.P6000 -> {
                prefill6000.prefill(prefillData)
                prefillData
            }

            ENUM_SED.P2000 -> {
                prefill2000.prefill(prefillData)
                prefillData
            }

            ENUM_SED.P4000 -> {
                //skal person data komme fra P4000? eller kun fra TPS?
                prefill4000.prefill(prefillData)
                prefillData
            }

            else -> {
                //"P2100","P2200","P5000" -> {
                return when {
                    validsed(enumSED.sed, STANDARD_SED) ->  {
                        prefillDefault.prefill(prefillData)
                        prefillData
                    }
                    else -> throw IllegalArgumentException("Mangler SED, eller ugyldig type SED")
                }
            }
        }
    }
}