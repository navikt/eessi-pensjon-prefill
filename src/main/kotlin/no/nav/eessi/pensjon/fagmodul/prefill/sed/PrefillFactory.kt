package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillGjenlevende
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2000
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2100
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2200
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.PrefillP6000
import no.nav.eessi.pensjon.fagmodul.prefill.tps.BrukerFromTPS
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PrefillFactory(private val prefillNav: PrefillNav,
                     private val dataFromTPS: BrukerFromTPS,
                     private val eessiInformasjon: EessiInformasjon,
                     private val dataFromPEN: PensjonsinformasjonHjelper) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillFactory::class.java) }

    fun createPrefillClass(prefillData: PrefillDataModel): Prefill<SED> {

        val sedValue = SEDType.valueOf(prefillData.getSEDid())

        logger.debug("mapping prefillClass to SED: $sedValue")

        return when (sedValue) {
            SEDType.P6000 -> {
                PrefillP6000(prefillNav, eessiInformasjon, dataFromPEN, dataFromTPS)
            }
            SEDType.P2000 -> {
                PrefillP2000(prefillNav, dataFromPEN, dataFromTPS)
            }
            SEDType.P2200 -> {
                PrefillP2200(prefillNav, dataFromPEN, dataFromTPS)
            }
            SEDType.P2100 -> {
                PrefillP2100(prefillNav, dataFromPEN, dataFromTPS)
            }
            SEDType.P4000 -> {
                PrefillP4000(getPrefillSed(prefillData))
            }
            SEDType.P7000 -> {
                PrefillP7000(getPrefillSed(prefillData))
            }
            SEDType.P8000 -> {
                PrefillP8000(getPrefillSed(prefillData))
            }
            SEDType.P10000 -> {
                PrefillP10000(getPrefillSed(prefillData))
            }
            SEDType.X005 -> {
                PrefillX005(prefillNav)
            }
            SEDType.H020, SEDType.H021 -> {
                PrefillH02X(getPrefillSed(prefillData))
            }
            else -> {
                //P3000_NO vil aldre gå dennee vei! men fra EU-SED->Nav-SED->PESYS
                //P3000_SE, PL, DK, DE, UK, ol vil gå denne veien.
                //P5000, - P9000, P14000 og og andre
                PrefillDefaultSED(getPrefillSed(prefillData))
            }
        }
    }

    private fun getPrefillSed(prefillData: PrefillDataModel) : PrefillSed {
        val pensjonGjenlevende = PrefillGjenlevende(dataFromTPS, prefillNav).prefill(prefillData)
        return PrefillSed(prefillNav, pensjonGjenlevende)
    }


}
