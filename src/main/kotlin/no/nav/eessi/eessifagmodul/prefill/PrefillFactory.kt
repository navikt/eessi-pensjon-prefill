package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.prefill.vedtak.PrefillP6000
import no.nav.eessi.eessifagmodul.utils.ALL_SED
import no.nav.eessi.eessifagmodul.utils.SedEnum
import no.nav.eessi.eessifagmodul.utils.validsed
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PrefillFactory {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillFactory::class.java) }

    @Autowired
    lateinit var prefill6000: PrefillP6000

    @Autowired
    lateinit var prefill2000: PrefillP2000

    @Autowired
    lateinit var prefill2100: PrefillP2100

    @Autowired
    lateinit var prefill2200: PrefillP2200

    @Autowired
    lateinit var prefill3000: PrefillP3000

    @Autowired
    lateinit var prefill4000: PrefillP4000

    @Autowired
    lateinit var prefillDefault: PrefillDefaultSED

    fun createPrefillClass(prefillData: PrefillDataModel): Prefill<SED> {
        val sedValue = SedEnum.valueOf(prefillData.getSEDid())
        logger.debug("mapping prefillClass to SED: $sedValue")

        return when(sedValue) {
            SedEnum.P6000 -> {
                //prefillDefault
                //TODO legge prefill6000 tilbake når klar til test
                prefill6000
            }
            SedEnum.P2000 -> {
                prefill2000
            }
            SedEnum.P2100 -> {
                prefill2100
            }
            SedEnum.P2200 -> {
                prefill2200
            }
            SedEnum.P3000 -> {
                prefill3000
            }
            SedEnum.P4000 -> {
                prefill4000
            }
            else -> {
                //"P5000" -> {
                return when {
                    validsed(sedValue.name, ALL_SED) ->  {
                        prefillDefault
                    }
                    else -> throw IllegalArgumentException("Mangler SED, eller ugyldig type SED")
                }
            }
        }
    }

}