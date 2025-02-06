package no.nav.eessi.pensjon.prefill.sed.vedtak.helper

import no.nav.eessi.pensjon.eux.model.sed.AvslagbegrunnelseItem
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PrefillPensjonVedtaksavslag {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPensjonVedtaksavslag::class.java) }

    fun sjekkForVilkarsvurderingListeHovedytelseellerAvslag(pendata: Pensjonsinformasjon): Boolean {
        try {
            val hovedytelseAvslag = pendata.vilkarsvurderingListe.vilkarsvurderingListe.first()
            if (hovedytelseAvslag.resultatHovedytelse == "AVSL" || hovedytelseAvslag.avslagHovedytelse == "AVSL") {
                return true
            }
        } catch (ex: Exception) {
            logger.error("Ingen vilkarsvurderingListe, sjekk på AVSL")
        }
        return false
    }

    /**
     * /4.1.13.1 - 4.1.13.2.1
     */
    fun createAvlsagsBegrunnelseItem(pendata: Pensjonsinformasjon): List<AvslagbegrunnelseItem>? {
        logger.info("4.1.13        AvlsagsBegrunnelseItem")

        val avslagbegrunnelse = createAvlsagsBegrunnelse(pendata)

        val item = listOf(
            AvslagbegrunnelseItem(

            //4.1.13.1
            begrunnelse = avslagbegrunnelse,
        )
        )

        if (avslagbegrunnelse == null)
            return null

        return item
    }

    /**
     *  Kodeverk pr. jan 2021 https://confluence.adeo.no/pages/viewpage.action?pageId=338181329
     *  4.1.13.1 - Rejection reasons
     *
     *          4.1.[1].13.[1].1. Avslagsgrunner
     *          [01] Ingen forsikringsperioder
     *          [02] Forsikringsperiode på mindre enn ett år
     *          [03] Krav til perioden eller andre kvalifiseringskrav er ikke oppfylt
     *          [04] Ingen delvis uførhet eller funksjonshemming ble funnet
     *          [05] Inntektsgrensen er overskredet
     *          [06] Pensjonsalder er ikke nådd
     *          [07] Manglende innformasjon fra søkeren
     *          [08] Manglende deltakelse
     *          [99] Andre grunner
     *
     */
    fun createAvlsagsBegrunnelse(pendata: Pensjonsinformasjon): String? {
        logger.info("4.1.13.1          AvlsagsBegrunnelse")

        if (pendata.vilkarsvurderingListe == null || pendata.vilkarsvurderingListe.vilkarsvurderingListe == null) {
            return null
        }
        val sakType = KSAK.valueOf(pendata.sakAlder.sakType)

        val erAvslagVilkarsproving = VedtakPensjonDataHelper.hentVilkarsResultatHovedytelse(pendata) == "AVSL"

        val harBoddArbeidetUtland = VedtakPensjonDataHelper.harBoddArbeidetUtland(pendata)
        val erTrygdetidListeTom = pendata.trygdetidListe.trygdetidListe.isEmpty()

        val erLavtTidligUttak = VedtakPensjonDataHelper.isVilkarsvurderingAvslagHovedytelseSamme("LAVT_TIDLIG_UTTAK", pendata)
        val erUnder62 = VedtakPensjonDataHelper.isVilkarsvurderingAvslagHovedytelseSamme("UNDER_62", pendata)
        val erIkkeMottattDok = "IKKE_MOTTATT_DOK" == VedtakPensjonDataHelper.hentVilkarsProvingAvslagHovedYtelse(pendata)
        val erMindreEnn3aar = "UNDER_3_AR_TT" == VedtakPensjonDataHelper.hentVilkarsProvingAvslagHovedYtelse(pendata)
        val erMindreEnn1aar = "UNDER_1_AR_TT" == VedtakPensjonDataHelper.hentVilkarsProvingAvslagHovedYtelse(pendata)

        //UFOREP
        val erForutMedlem = "FORUT_MEDL" == VedtakPensjonDataHelper.hentVilkarsvurderingUforetrygd(pendata).unntakForutgaendeMedlemskap
        val erHensArbrettTiltak = "HENS_ARBRETT_TILTAK" == VedtakPensjonDataHelper.hentVilkarsvurderingUforetrygd(pendata).hensiktsmessigArbeidsrettedeTiltak
        val erHensiktmessigBeh = "HENSIKTSMESSIG_BEH" == VedtakPensjonDataHelper.hentVilkarsvurderingUforetrygd(pendata).hensiktsmessigBehandling
        val erNedsattInntEvne = "NEDSATT_INNT_EVNE" == VedtakPensjonDataHelper.hentVilkarsvurderingUforetrygd(pendata).nedsattInntektsevne
        val erAlder = "ALDER" == VedtakPensjonDataHelper.hentVilkarsvurderingUforetrygd(pendata).alder

        when {
            harBoddArbeidetUtland -> {
                when {
                    KSAK.UFOREP == sakType -> {
                        when {
                            erAlder && erAvslagVilkarsproving -> return "03"
                            (erHensiktmessigBeh || erHensArbrettTiltak) && erAvslagVilkarsproving -> return "08"
                            erNedsattInntEvne && erAvslagVilkarsproving -> return "04"
                            VedtakPensjonDataHelper.erTrygdeTid(pendata) && erForutMedlem && erAvslagVilkarsproving -> return "02"
                            pendata.trygdetidListe.trygdetidListe.isEmpty() && erForutMedlem && erAvslagVilkarsproving -> return "01"
                        }
                    }
                    erAvslagVilkarsproving -> {
                        //pkt1 og pkt.9
                        when {
                            erTrygdetidListeTom && !erMindreEnn3aar && !erMindreEnn1aar -> return "01"
                            erMindreEnn1aar || VedtakPensjonDataHelper.erTrygdeTid(pendata) -> return "02"
                            erLavtTidligUttak || erMindreEnn3aar -> return "03"
                            erUnder62 -> return "06"
                        }
                    }
                }
                when {
                    erIkkeMottattDok && erAvslagVilkarsproving -> return "07"
                }
            }
        }

        logger.info("              -- Ingen avslagsbegrunnelse")
        return null
    }
}