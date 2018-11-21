package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.SEDType
import no.nav.eessi.eessifagmodul.prefill.krav.PrefillP2000
import no.nav.eessi.eessifagmodul.prefill.nav.PrefillNav
import no.nav.eessi.eessifagmodul.prefill.nav.PrefillPerson
import no.nav.eessi.eessifagmodul.prefill.nav.PrefillPersonDataFromTPS
import no.nav.eessi.eessifagmodul.prefill.vedtak.PrefillP6000
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PrefillFactory(private val prefillNav: PrefillNav, private val dataFromTPS: PrefillPersonDataFromTPS, private val dataFromPEN: PensjonsinformasjonHjelper) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillFactory::class.java) }

    private val prefillPerson = PrefillPerson(prefillNav, PrefillPensjon(dataFromTPS))

    fun createPrefillClass(prefillData: PrefillDataModel): Prefill<SED> {
        val sedValue = SEDType.valueOf(prefillData.getSEDid())

        logger.debug("mapping prefillClass to SED: $sedValue")

        return when (sedValue) {
            //Status hva gjendstår
            SEDType.P6000 -> {
                PrefillP6000(prefillNav, dataFromTPS, dataFromPEN)
            }
            //Status hva gjendstår
            SEDType.P2000 -> {
                PrefillP2000(prefillNav, dataFromTPS, dataFromPEN)
            }
            //Status hva gjendstår
            SEDType.P2200 -> {
                PrefillP2000(prefillNav, dataFromTPS, dataFromPEN)
            }
            SEDType.P2100 -> {
                PrefillP2100(prefillPerson)
            }
            //P3000_NO vil aldre gå dennee vei! men fra EU-SED->Nav-SED->PESYS
            SEDType.P3000 -> {
                PrefillP3000(prefillPerson)
            }
            SEDType.P4000 -> {
                PrefillP4000(prefillPerson)
            }

            else -> {
                //P5000, P8000, P7000
                PrefillDefaultSED(prefillPerson)
            }

        }
    }


}