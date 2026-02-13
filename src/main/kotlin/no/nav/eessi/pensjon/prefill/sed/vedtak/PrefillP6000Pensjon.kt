package no.nav.eessi.pensjon.prefill.sed.vedtak

import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonReduksjon
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonSak
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonTilleggsinformasjon
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonVedtak
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.VedtakPensjonDataHelper.harBoddArbeidetUtland
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException


/**
 * Hjelpe klasse for utfyller ut NAV-SED-P6000 med pensjondata med vedtak fra PESYS.
 */
object PrefillP6000Pensjon {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP6000Pensjon::class.java) }

    fun prefillP6000Pensjon(
        pesysPrefillData: P6000MeldingOmVedtakDto,
        gjenlevende: Bruker?,
        andreinstitusjonerItem: AndreinstitusjonerItem?
    ): P6000Pensjon {

        //Sjekk opp om det er Bodd eller Arbeid utland. (hvis ikke avslutt)
        if (!harBoddArbeidetUtland(pesysPrefillData))
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Har ikke bodd eller arbeidet i utlandet. Avbryter opprettelse av SED")

        //Sjekk opp om det finnes et dato fattet vedtak. (hvis ikke avslutt)
        if (pesysPrefillData.vedtak.datoFattetVedtak == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Vedtaket mangler dato for FattetVedtak. Avbryter opprettelse av SED")
        }

        //prefill Pensjon obj med data fra PESYS. (pendata)
        logger.debug("4.1       VedtakItem")

        return if (erAvslag(pesysPrefillData)) {
            prefillPensjonMedAvslag(pesysPrefillData, andreinstitusjonerItem)
        } else {
            P6000Pensjon(
                gjenlevende = gjenlevende,
                //4.1
                vedtak = prefillVedtak(pesysPrefillData),
                //5.1
                reduksjon = prefillReduksjon(pesysPrefillData),
                //6.1
                sak = prefillSak(pesysPrefillData),
                //6.x
                tilleggsinformasjon = prefillTilleggsinformasjon(pesysPrefillData, andreinstitusjonerItem)
            )
        }
    }

    private fun prefillPensjonMedAvslag(
        pesysPrefillData: P6000MeldingOmVedtakDto,
        andreinstitusjonerItem: AndreinstitusjonerItem?
    ): P6000Pensjon {
        logger.warn("Avslag, Ingen vilkarsvurderingListe og ytelsePerMaanedListe oppretter Vedtak SED P6000 uten pensjoninformasjon")

        val vedtak = prefillVedtak(pesysPrefillData).firstOrNull()
        val vedtaklist = if (vedtak == null) null else listOf(
            VedtakItem(
                type = vedtak.type,
                resultat = vedtak.resultat,
                avslagbegrunnelse = vedtak.avslagbegrunnelse
            )
        )

        return P6000Pensjon(
            vedtak = vedtaklist,
            sak = prefillSak(pesysPrefillData),
            tilleggsinformasjon = prefillTilleggsinformasjon(pesysPrefillData, andreinstitusjonerItem)
        )
    }

    private fun prefillTilleggsinformasjon(
        pensjoninformasjon: P6000MeldingOmVedtakDto,
        andreinstitusjonerItem: AndreinstitusjonerItem?
    ): Tilleggsinformasjon? {
        return try {
            PrefillPensjonTilleggsinformasjon.createTilleggsinformasjon(pensjoninformasjon, andreinstitusjonerItem)
        } catch (ex: Exception) {
            logger.warn("Feilet ved preutfylling tilleggsinformasjon, fortsetter uten, feilmelding: ${ex.message}")
            null
        }
    }

    private fun prefillSak(pensjoninformasjon: P6000MeldingOmVedtakDto): Sak? {
        return try {
            PrefillPensjonSak.createSak(pensjoninformasjon)
        } catch (ex: Exception) {
            logger.warn("Feilet ved preutfylling av sak, fortsetter uten, feilmelding: ${ex.message}")
            null
        }
    }

    private fun prefillReduksjon(pensjoninformasjon: P6000MeldingOmVedtakDto): List<ReduksjonItem>? {
        return try {
            PrefillPensjonReduksjon.createReduksjon(pensjoninformasjon)
        } catch (ex: Exception) {
            logger.warn("Feilet ved preutfylling av reduksjoner, fortsetter uten, feilmelding: ${ex.message}")
            emptyList()
        }
    }

    private fun prefillVedtak(pesysPrefillData: P6000MeldingOmVedtakDto): List<VedtakItem> {
        return try {
            listOf(PrefillPensjonVedtak.createVedtakItem(pesysPrefillData))
        } catch (ex: Exception) {
            logger.warn("Feilet ved preutfylling av vedtaksdetaljer, fortsetter uten, feilmelding: ${ex.message}")
            emptyList()
        }
    }

    private fun erAvslag(pesysPrefillData: P6000MeldingOmVedtakDto): Boolean {
        val vilkar = pesysPrefillData.vilkarsvurdering
        val ytelse = pesysPrefillData.ytelsePerMaaned
        val erAvslag = "AVSL" == pesysPrefillData.vilkarsvurdering.maxByOrNull { it.fom }?.avslagHovedytelse
        return (erAvslag || (vilkar.isEmpty() && ytelse.isEmpty()) || ytelse.isEmpty())
    }
}
