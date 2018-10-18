package no.nav.eessi.eessifagmodul.prefill.P6000

import no.nav.eessi.eessifagmodul.models.IkkeGyldigKallException
import no.nav.eessi.eessifagmodul.models.Pensjon
import no.nav.eessi.eessifagmodul.prefill.Prefill
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component


@Component
/**
 * Hjelpe klasse for P6000 som fyller ut NAV-SED med pensjondata fra PESYS.
 */
class PensionDataFromPESYS(private val pensjonsinformasjonService: PensjonsinformasjonService): PensjonData(), Prefill<Pensjon>  {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PensionDataFromPESYS::class.java) }

    final var reduksjon: PrefillPensjonReduksjon

    final var tilleggsinformasjon: PrefillPensjonTilleggsinformasjon

    final  var pensjonSak: PrefillPensjonSak

    final  var pensjonVedtak: PrefillPensjonVedtak

    init {
        logger.debug("\nLaster opp hjelperklasser for preutfylling.")

        reduksjon = PrefillPensjonReduksjon()
        tilleggsinformasjon = PrefillPensjonTilleggsinformasjon()
        pensjonSak = PrefillPensjonSak()
        pensjonVedtak = PrefillPensjonVedtak()

        logger.debug("Ferdig med å laste inn hjelpeklasser\n")
    }

    fun getPensjoninformasjonFraVedtak(vedtakId: String): Pensjonsinformasjon {
        val pendata: Pensjonsinformasjon = pensjonsinformasjonService.hentAlt(vedtakId) // ha med saknr og vedtak?
        /*
            logger.debug("Pensjonsinformasjon: $pendata")
            logger.debug("Pensjonsinformasjon.vedtak: ${pendata.vedtak}")
            logger.debug("Pensjonsinformasjon.vedtak.virkningstidspunkt: ${pendata.vedtak.virkningstidspunkt}")
            logger.debug("Pensjonsinformasjon.sak: ${pendata.sak}")
            logger.debug("Pensjonsinformasjon.trygdetidListe: ${pendata.trygdetidListe}")
            logger.debug("Pensjonsinformasjon.vilkarsvurderingListe: ${pendata.vilkarsvurderingListe}")
            logger.debug("Pensjonsinformasjon.ytelsePerMaanedListe: ${pendata.ytelsePerMaanedListe}")
            logger.debug("Pensjonsinformasjon.trygdeavtale: ${pendata.trygdeavtale}")
            logger.debug("Pensjonsinformasjon.person: ${pendata.person}")
            logger.debug("Pensjonsinformasjon.person.pin: ${pendata.person.pid}")
        */
        return pendata
    }

    override fun prefill(prefillData: PrefillDataModel): Pensjon {
        //Kast exception dersom vedtakId mangler
        val vedtakId = if ( prefillData.vedtakId.isNotBlank() ) prefillData.vedtakId else throw IkkeGyldigKallException("Mangler vedtakID")

        logger.debug("----------------------------------------------------------")
        val starttime = System.nanoTime()

        logger.debug("Starter [P6000] Preutfylling Utfylling Data")

        logger.debug("vedtakId: $vedtakId")
        logger.debug("Henter pensjondata fra PESYS")
        val pendata: Pensjonsinformasjon = getPensjoninformasjonFraVedtak(prefillData.vedtakId)

        val endtime = System.nanoTime()
        val tottime = endtime - starttime

        logger.debug("Metrics")
        logger.debug("Ferdig hentet pensjondata fra PESYS. Det tok ${(tottime/1.0e9)} sekunder.")
        logger.debug("----------------------------------------------------------")

        if (!harBoddArbeidetUtland(pendata)) throw IkkeGyldigKallException("Har ikke bodd eller arbeidet i utlandet. Avslutter P6000")

        //prefill Pensjon obj med data fra PESYS. (pendata)
        return Pensjon(
                //4.1
                vedtak = listOf( pensjonVedtak.createVedtakItem(pendata)),
                //5.1
                reduksjon = reduksjon.createReduksjon(pendata),
                //6.1
                sak = pensjonSak.createSak(pendata),
                //6.x
                tilleggsinformasjon = tilleggsinformasjon.createTilleggsinformasjon(pendata)
        )

    }

}

