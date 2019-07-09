package no.nav.eessi.pensjon.fagmodul.prefill.vedtak

import no.nav.eessi.pensjon.fagmodul.models.IkkeGyldigKallException
import no.nav.eessi.pensjon.fagmodul.models.Pensjon
import no.nav.eessi.pensjon.fagmodul.prefill.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillDataModel
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Hjelpe klasse for utfyller ut NAV-SED-P6000 med pensjondata med vedtak fra PESYS.
 */
class VedtakDataFromPEN(private val dataFromPESYS: PensjonsinformasjonHjelper) : VedtakPensjonData(), Prefill<Pensjon> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(VedtakDataFromPEN::class.java) }

    private val reduksjon: PrefillPensjonReduksjon
    val tilleggsinformasjon: PrefillPensjonTilleggsinformasjon
    private val pensjonSak: PrefillPensjonSak
    val pensjonVedtak: PrefillPensjonVedtak

    init {
        logger.debug("\nLaster opp hjelperklasser for preutfylling.")

        reduksjon = PrefillPensjonReduksjon()
        tilleggsinformasjon = PrefillPensjonTilleggsinformasjon()
        pensjonSak = PrefillPensjonSak()
        pensjonVedtak = PrefillPensjonVedtak()

        logger.debug("Ferdig med å laste inn hjelpeklasser\n")
    }

    //henter ut felles pen-data og kun pensjoninformasjon med vedtak
    fun getPensjoninformasjonFraVedtak(prefillData: PrefillDataModel): Pensjonsinformasjon {
        return dataFromPESYS.hentMedVedtak(prefillData)
    }

    override fun prefill(prefillData: PrefillDataModel): Pensjon {
        //Kast exception dersom vedtakId mangler
        val vedtakId = if (prefillData.vedtakId.isNotBlank()) prefillData.vedtakId else throw IkkeGyldigKallException("Mangler vedtakID")

        logger.debug("----------------------------------------------------------")
        val starttime = System.nanoTime()

        logger.debug("Starter [vedtak] Preutfylling Utfylling Data")

        logger.debug("vedtakId: $vedtakId")
        val pendata: Pensjonsinformasjon = getPensjoninformasjonFraVedtak(prefillData)
        logger.debug("Henter pensjondata fra PESYS")

        val endtime = System.nanoTime()
        val tottime = endtime - starttime

        logger.debug("Metrics")
        logger.debug("Ferdig hentet pensjondata fra PESYS. Det tok ${(tottime / 1.0e9)} sekunder.")
        logger.debug("----------------------------------------------------------")

        //Sjekk opp om det er Bodd eller Arbeid utland. (hvis ikke avslutt)
        if (!harBoddArbeidetUtland(pendata)) throw IkkeGyldigKallException("Har ikke bodd eller arbeidet i utlandet. Avslutter vedtak")
        //Sjekk opp om det finnes et dato fattet vedtak. (hvis ikke avslutt)
        if (pendata.vedtak.datoFattetVedtak == null) {
            throw IkkeGyldigKallException("Vedak mangler dato for fatet vedtak. Avslutter")
        }

        //prefill Pensjon obj med data fra PESYS. (pendata)
        return Pensjon(
                //4.1
                vedtak = listOf(pensjonVedtak.createVedtakItem(pendata)),
                //5.1
                reduksjon = reduksjon.createReduksjon(pendata),
                //6.1
                sak = pensjonSak.createSak(pendata),
                //6.x
                tilleggsinformasjon = tilleggsinformasjon.createTilleggsinformasjon(pendata, prefillData)
        )
    }
}

