package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import no.nav.eessi.pensjon.fagmodul.sedmodel.Pensjon
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.VedtakPensjonDataHelper.harBoddArbeidetUtland
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.PrefillPensjonReduksjon
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.PrefillPensjonSak
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.PrefillPensjonTilleggsinformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.PrefillPensjonVedtak
import no.nav.eessi.pensjon.fagmodul.sedmodel.AndreinstitusjonerItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.IllegalStateException


/**
 * Hjelpe klasse for utfyller ut NAV-SED-P6000 med pensjondata med vedtak fra PESYS.
 */
object PrefillP6000Pensjon {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP6000Pensjon::class.java) }

    fun createPensjon(dataFromPESYS: PensjonsinformasjonHjelper, gjenlevende: Bruker?, vedtakId: String, andreinstitusjonerItem: AndreinstitusjonerItem?): Pensjon {

        if (vedtakId.isBlank()) throw IkkeGyldigStatusPaaSakException("Mangler vedtakID")

        logger.debug("----------------------------------------------------------")
        val starttime = System.nanoTime()

        logger.debug("Starter [vedtak] Preutfylling Utfylling Data")

        logger.debug("vedtakId: $vedtakId")
        val pendata: Pensjonsinformasjon = dataFromPESYS.hentMedVedtak(vedtakId)

        logger.debug("Henter pensjondata fra PESYS")

        val endtime = System.nanoTime()
        val tottime = endtime - starttime

        logger.debug("Metrics")
        logger.debug("Ferdig hentet pensjondata fra PESYS. Det tok ${(tottime / 1.0e9)} sekunder.")
        logger.debug("----------------------------------------------------------")

        //Sjekk opp om det er Bodd eller Arbeid utland. (hvis ikke avslutt)
        if (!harBoddArbeidetUtland(pendata)) throw IkkeGyldigStatusPaaSakException("Har ikke bodd eller arbeidet i utlandet. Avslutter vedtak")
        //Sjekk opp om det finnes et dato fattet vedtak. (hvis ikke avslutt)
        if (pendata.vedtak.datoFattetVedtak == null) {
            throw IkkeGyldigStatusPaaSakException("Vedtaket mangler dato for FattetVedtak. Avslutter")
        }

        //prefill Pensjon obj med data fra PESYS. (pendata)
        logger.debug("4.1       VedtakItem")
        return Pensjon(
                gjenlevende = gjenlevende,
                //4.1
                vedtak = listOf(PrefillPensjonVedtak.createVedtakItem(pendata)),
                //5.1
                reduksjon = PrefillPensjonReduksjon.createReduksjon(pendata),
                //6.1
                sak = PrefillPensjonSak.createSak(pendata),
                //6.x
                tilleggsinformasjon = PrefillPensjonTilleggsinformasjon.createTilleggsinformasjon(pendata, andreinstitusjonerItem)
        )
    }
}

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class IkkeGyldigStatusPaaSakException(message: String) : IllegalStateException(message)
