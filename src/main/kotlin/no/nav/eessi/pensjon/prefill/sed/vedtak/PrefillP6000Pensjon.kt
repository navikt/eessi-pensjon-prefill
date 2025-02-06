package no.nav.eessi.pensjon.prefill.sed.vedtak

import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteResponse
import no.nav.eessi.pensjon.prefill.etterlatte.Perioden
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonReduksjon
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonSak
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonTilleggsinformasjon
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonVedtak
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.VedtakPensjonDataHelper.harBoddArbeidetUtland
import no.nav.eessi.pensjon.utils.simpleFormat
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.format.DateTimeFormatter


/**
 * Hjelpe klasse for utfyller ut NAV-SED-P6000 med pensjondata med vedtak fra PESYS.
 */
object PrefillP6000Pensjon {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP6000Pensjon::class.java) }

    fun prefillP6000Pensjon(
        pensjoninformasjon: Pensjonsinformasjon,
        gjenlevende: Bruker?,
        andreinstitusjonerItem: AndreinstitusjonerItem?
    ): P6000Pensjon {

        //Sjekk opp om det er Bodd eller Arbeid utland. (hvis ikke avslutt)
        if (!harBoddArbeidetUtland(pensjoninformasjon))
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Har ikke bodd eller arbeidet i utlandet. Avbryter oppretelse av SED")

        //Sjekk opp om det finnes et dato fattet vedtak. (hvis ikke avslutt)
        if (pensjoninformasjon.vedtak.datoFattetVedtak == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Vedtaket mangler dato for FattetVedtak. Avbryter oppretelse av SED")
        }

        //prefill Pensjon obj med data fra PESYS. (pendata)
        logger.debug("4.1       VedtakItem")

        return if (erAvslag(pensjoninformasjon)) {
            prefillPensjonMedAvslag(pensjoninformasjon, andreinstitusjonerItem)
        } else {
            return P6000Pensjon(
                gjenlevende = gjenlevende,
                //4.1
                vedtak = prefillVedtak(pensjoninformasjon),
                //5.1
                reduksjon = prefillReduksjon(pensjoninformasjon),
                //6.1
                sak = prefillSak(pensjoninformasjon),
                //6.x
                tilleggsinformasjon = prefillTilleggsinformasjon(pensjoninformasjon, andreinstitusjonerItem)
            )
        }
    }

    fun prefillP6000PensjonVedtak(
        gjenlevende: Bruker?,
        etterlatteResponse: EtterlatteResponse?,
        andreinstitusjonerItem: AndreinstitusjonerItem?
    ): P6000Pensjon {
        val simpleFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return P6000Pensjon(
            gjenlevende = gjenlevende,
            sak = Sak(kravtype = listOf(KravtypeItem(datoFrist = "six weeks from the date the decision is received", krav = etterlatteResponse?.sakType))),
            vedtak = etterlatteResponse?.virkningstidspunkt?.let {
                val utbetalingEtterlatte = etterlatteResponse.utbetaling?.first()
                listOf(
                    VedtakItem(
                        virkningsdato = simpleFormatter.format(it),
                        type = etterlatteResponse.type,
                        beregning = utbetalingEtterlatte?.let {
                            listOf(
                                BeregningItem(
                                    periode = Periode(fom = simpleFormatter.format(utbetalingEtterlatte.periode.start), tom = simpleFormatter.format(utbetalingEtterlatte.periode.end)),
                                    beloepBrutto = BeloepBrutto(beloep = utbetalingEtterlatte.beloep),
                                    valuta = "NOK",
                                    utbetalingshyppighet = "maaned_12_per_aar"
                                )
                            )
                        }
                    )
                )
            },
            reduksjon = null,
            tilleggsinformasjon = andreinstitusjonerItem?.let { Tilleggsinformasjon(andreinstitusjoner = listOf(andreinstitusjonerItem)) }
        )
    }

//    fun prefillP6000PensjonGjenny(
//        vedtakInformasjonGjenny: EtterlatteResponse,
//        gjenlevende: Bruker?,
//        andreinstitusjonerItem: AndreinstitusjonerItem?
//    ): P6000Pensjon {
//        logger.debug("4.1       VedtaksInfo fra gjenny")
//
//        return if (vedtakInformasjonGjenny.sakType == "AVSLAG") {) {
//            VedtakItem(
//                    type = vedtak.type,
//            resultat = vedtak.resultat,
//            avslagbegrunnelse = vedtak.avslagbegrunnelse
//            )
//        } else {
//            return P6000Pensjon(
//                gjenlevende = gjenlevende,
//                vedtak = prefillVedtak(vedtakInformasjonGjenny),
//
//            )
//        }
//    }


    private fun prefillPensjonMedAvslag(
        pensjoninformasjon: Pensjonsinformasjon,
        andreinstitusjonerItem: AndreinstitusjonerItem?
    ): P6000Pensjon {
        logger.warn("Avslag, Ingen vilkarsvurderingListe og ytelsePerMaanedListe oppretter Vedtak SED P6000 uten pensjoninformasjon")

        val vedtak = prefillVedtak(pensjoninformasjon).firstOrNull()
        val vedtaklist = if (vedtak == null) null else listOf(
            VedtakItem(
                type = vedtak.type,
                resultat = vedtak.resultat,
                avslagbegrunnelse = vedtak.avslagbegrunnelse
            )
        )

        return P6000Pensjon(
            vedtak = vedtaklist,
            sak = prefillSak(pensjoninformasjon),
            tilleggsinformasjon = prefillTilleggsinformasjon(pensjoninformasjon, andreinstitusjonerItem)
        )
    }

    private fun prefillTilleggsinformasjon(
        pensjoninformasjon: Pensjonsinformasjon,
        andreinstitusjonerItem: AndreinstitusjonerItem?
    ): Tilleggsinformasjon? {
        return try {
            PrefillPensjonTilleggsinformasjon.createTilleggsinformasjon(pensjoninformasjon, andreinstitusjonerItem)
        } catch (ex: Exception) {
            logger.warn("Feilet ved preutfylling tilleggsinformasjon, fortsetter uten, feilmelding: ${ex.message}")
            null
        }
    }

    private fun prefillSak(pensjoninformasjon: Pensjonsinformasjon): Sak? {
        return try {
            PrefillPensjonSak.createSak(pensjoninformasjon)
        } catch (ex: Exception) {
            logger.warn("Feilet ved preutfylling av sak, fortsetter uten, feilmelding: ${ex.message}")
            null
        }
    }

    private fun prefillReduksjon(pensjoninformasjon: Pensjonsinformasjon): List<ReduksjonItem>? {
        return try {
            PrefillPensjonReduksjon.createReduksjon(pensjoninformasjon)
        } catch (ex: Exception) {
            logger.warn("Feilet ved preutfylling av reduksjoner, fortsetter uten, feilmelding: ${ex.message}")
            emptyList()
        }
    }

    private fun prefillVedtak(pensjoninformasjon: Pensjonsinformasjon): List<VedtakItem> {
        return try {
            listOf(PrefillPensjonVedtak.createVedtakItem(pensjoninformasjon))
        } catch (ex: Exception) {
            logger.warn("Feilet ved preutfylling av vedtaksdetaljer, fortsetter uten, feilmelding: ${ex.message}")
            emptyList()
        }
    }

//    private fun prefillVedtakGjenny(vedtakInfoFraGjenny: EtterlatteResponse): List<VedtakItem> {
//        return try {
//            listOf(PrefillPensjonVedtak.createVedtakItem(pensjoninformasjon))
//        } catch (ex: Exception) {
//            logger.warn("Feilet ved preutfylling av vedtaksdetaljer, fortsetter uten")
//            emptyList()
//        }
//    }

    private fun erAvslag(pensjoninformasjon: Pensjonsinformasjon): Boolean {
        val vilkar = pensjoninformasjon.vilkarsvurderingListe
        val ytelse = pensjoninformasjon.ytelsePerMaanedListe
        val erAvslag =
            "AVSL" == pensjoninformasjon.vilkarsvurderingListe?.vilkarsvurderingListe?.maxByOrNull { it.fom.simpleFormat() }?.avslagHovedytelse
        return (erAvslag || (vilkar == null && ytelse == null) || ytelse.ytelsePerMaanedListe.isNullOrEmpty())
    }
}
