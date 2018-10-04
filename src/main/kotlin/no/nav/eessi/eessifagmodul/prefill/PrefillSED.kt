package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.utils.STANDARD_SED
import no.nav.eessi.eessifagmodul.utils.SedEnum
import no.nav.eessi.eessifagmodul.utils.validsed
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
/**
 * Prefill factory deligate sed to dedicated prefillclass
 */
class PrefillSED: Prefill<PrefillDataModel> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillSED::class.java) }

    @Autowired
    lateinit var prefill6000: PrefillP6000

    @Autowired
    lateinit var prefill2000: PrefillP2000

    @Autowired
    lateinit var prefill2100: PrefillP2100

    @Autowired
    lateinit var prefill2200: PrefillP2200

    @Autowired
    lateinit var prefill4000: PrefillP4000

    @Autowired
    lateinit var prefillDefault: PrefillDefaultSED

    override fun prefill(prefillData: PrefillDataModel): PrefillDataModel {

        val prefilling = createClass(prefillData)
        logger.debug("mapping prefillClass ${prefilling.javaClass}")
        prefilling.prefill(prefillData)
        return prefillData

//        val sedValue = SedEnum.valueOf(prefillData.getSEDid())
//        return when(sedValue) {
//            SedEnum.P6000 -> {
//                prefill6000.prefill(prefillData)
//                prefillData
//            }
//
//            SedEnum.P2000 -> {
//                prefill2000.prefill(prefillData)
//                prefillData
//            }
//            SedEnum.P2100 -> {
//                prefill2100.prefill(prefillData)
//                prefillData
//            }
//            SedEnum.P2200 -> {
//                prefill2200.prefill(prefillData)
//                prefillData
//            }
//
//            SedEnum.P4000 -> {
//                //skal person data komme fra P4000? eller kun fra TPS?
//                prefill4000.prefill(prefillData)
//                prefillData
//            }
//
//            else -> {
//                //"P5000" -> {
//                return when {
//                    validsed(sedValue.sed, STANDARD_SED) ->  {
//                        prefillDefault.prefill(prefillData)
//                        prefillData
//                    }
//                    else -> throw IllegalArgumentException("Mangler SED, eller ugyldig type SED")
//                }
//            }
//        }

    }

    fun createClass(prefillData: PrefillDataModel): Prefill<SED> {
        val sedValue = SedEnum.valueOf(prefillData.getSEDid())
        logger.debug("mapping prefillClass to SED: ${sedValue.sed}")
        return when(sedValue) {
            SedEnum.P6000 -> {
                prefill6000
//                prefill6000.prefill(prefillData)
//                prefillData
            }

            SedEnum.P2000 -> {
                prefill2000
//                prefill2000.prefill(prefillData)
//                prefillData
            }
            SedEnum.P2100 -> {
                prefill2100
//                prefill2100.prefill(prefillData)
//                prefillData
            }
            SedEnum.P2200 -> {
                prefill2200
//                prefill2200.prefill(prefillData)
//                prefillData
            }

            SedEnum.P4000 -> {
                prefill4000
                //skal person data komme fra P4000? eller kun fra TPS?
//                prefill4000.prefill(prefillData)
//                prefillData
            }

            else -> {
                //"P5000" -> {
                return when {
                    validsed(sedValue.sed, STANDARD_SED) ->  {
                        prefillDefault
//                        prefillDefault.prefill(prefillData)
//                        prefillData
                    }
                    else -> throw IllegalArgumentException("Mangler SED, eller ugyldig type SED")
                }
            }
        }
    }

}