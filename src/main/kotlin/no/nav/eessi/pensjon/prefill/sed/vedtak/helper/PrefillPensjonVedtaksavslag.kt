package no.nav.eessi.pensjon.prefill.sed.vedtak.helper

import no.nav.eessi.pensjon.eux.model.sed.AvslagbegrunnelseItem
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PrefillPensjonVedtaksavslag {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPensjonVedtaksavslag::class.java) }

    fun sjekkForVilkarsvurderingListeHovedytelseellerAvslag(pendata: P6000MeldingOmVedtakDto): Boolean {
        try {
            val hovedytelseAvslag = pendata.vilkarsvurderingListe.first()
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
    fun createAvlsagsBegrunnelseItem(pendata: P6000MeldingOmVedtakDto): List<AvslagbegrunnelseItem>? {
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
    fun createAvlsagsBegrunnelse(pendata: P6000MeldingOmVedtakDto): String? {
        logger.info("4.1.13.1          AvlsagsBegrunnelse")

        if (pendata.vilkarsvurderingListe.isEmpty()) {
            return null
        }
        val sakType = pendata.sakAlder.sakType

        val erAvslagVilkarsproving = VedtakPensjonDataHelper.hentVilkarsResultatHovedytelse(pendata) == "AVSL"

        val harBoddArbeidetUtland = VedtakPensjonDataHelper.harBoddArbeidetUtland(pendata)
        val erTrygdetidListeTom = pendata.trygdetidListe.isEmpty()

        val erLavtTidligUttak = VedtakPensjonDataHelper.isVilkarsvurderingAvslagHovedytelseSamme("LAVT_TIDLIG_UTTAK", pendata)
        val erUnder62 = VedtakPensjonDataHelper.isVilkarsvurderingAvslagHovedytelseSamme("UNDER_62", pendata)
        val erIkkeMottattDok = "IKKE_MOTTATT_DOK" == VedtakPensjonDataHelper.hentVilkarsProvingAvslagHovedYtelse(pendata)
        val erMindreEnn3aar = "UNDER_3_AR_TT" == VedtakPensjonDataHelper.hentVilkarsProvingAvslagHovedYtelse(pendata)
        val erMindreEnn1aar = "UNDER_1_AR_TT" == VedtakPensjonDataHelper.hentVilkarsProvingAvslagHovedYtelse(pendata)

        //UFOREP
        val vilkarsvurderingUforetrygd = VedtakPensjonDataHelper.hentVilkarsvurderingUforetrygd(pendata)
        val erForutMedlem = vilkarsvurderingUforetrygd
            ?.unntakForutgaendeMedlemskap == "FORUT_MEDL"
        val erHensArbrettTiltak = vilkarsvurderingUforetrygd
            ?.hensiktsmessigArbeidsrettedeTiltak == "HENS_ARBRETT_TILTAK"
        val erHensiktmessigBeh = vilkarsvurderingUforetrygd
            ?.hensiktsmessigBehandling == "HENSIKTSMESSIG_BEH"
        val erNedsattInntEvne = vilkarsvurderingUforetrygd
            ?.nedsattInntektsevne == "NEDSATT_INNT_EVNE"
        val erAlder = vilkarsvurderingUforetrygd
            ?.alder == "ALDER"

        when {
            harBoddArbeidetUtland -> {
                when {
                    EessiFellesDto.EessiSakType.UFOREP == sakType -> {
                        when {
                            erAlder && erAvslagVilkarsproving -> return "03"
                            (erHensiktmessigBeh || erHensArbrettTiltak) && erAvslagVilkarsproving -> return "08"
                            erNedsattInntEvne && erAvslagVilkarsproving -> return "04"
                            VedtakPensjonDataHelper.erTrygdeTid(pendata) && erForutMedlem && erAvslagVilkarsproving -> return "02"
                            pendata.trygdetidListe.isEmpty() && erForutMedlem && erAvslagVilkarsproving -> return "01"
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