package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper

import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.VedtakPensjonDataHelper.erTrygdeTid
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.VedtakPensjonDataHelper.harBoddArbeidetUtland
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.VedtakPensjonDataHelper.hentVilkarsProvingAvslagHovedYtelse
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.VedtakPensjonDataHelper.hentVilkarsResultatHovedytelse
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.VedtakPensjonDataHelper.hentVilkarsvurderingUforetrygd
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.VedtakPensjonDataHelper.hentVinnendeBergeningsMetode
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.VedtakPensjonDataHelper.hentYtelseskomponentBelop
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.VedtakPensjonDataHelper.isForeldelos
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.VedtakPensjonDataHelper.isMottarMinstePensjonsniva
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.VedtakPensjonDataHelper.isVilkarsvurderingAvslagHovedytelseSamme
import no.nav.eessi.pensjon.fagmodul.sedmodel.AvslagbegrunnelseItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.BeloepBrutto
import no.nav.eessi.pensjon.fagmodul.sedmodel.BeregningItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.Grunnlag
import no.nav.eessi.pensjon.fagmodul.sedmodel.Opptjening
import no.nav.eessi.pensjon.fagmodul.sedmodel.Periode
import no.nav.eessi.pensjon.fagmodul.sedmodel.Ukjent
import no.nav.eessi.pensjon.fagmodul.sedmodel.VedtakItem
import no.nav.eessi.pensjon.utils.simpleFormat
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sakalder.V1SakAlder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import no.nav.pensjon.v1.ytelsepermaaned.V1YtelsePerMaaned
import kotlin.Exception

object PrefillPensjonVedtak {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPensjonVedtak::class.java) }

    /**
     *  4.1
     */
    fun createVedtakItem(pendata: Pensjonsinformasjon): VedtakItem {
        logger.debug("PrefillPensjonReduksjon")
        logger.debug("4.1       VedtakItem")

        return VedtakItem(

                //4.1.1  $pensjon.vedtak[x].type
                type = createVedtakTypePensionWithRule(pendata),

                //4.1.2  $pensjon.vedtak[x].basertPaa
                basertPaa = createVedtakGrunnlagPentionWithRule(pendata),

                //4.1.3.1 $pensjon.vedtak[x].basertPaaAnnen
                basertPaaAnnen = createVedtakAnnenTypePentionWithRule(pendata),

                //4.1.4 $pensjon.vedtak[x].resultat
                resultat = createTypeVedtakPentionWithRule(pendata),

                //4.1.5 $pensjon.vedtak[x].artikkel
                artikkel = null,

                //4.1.6  $pensjon.vedtak[x].virkningsdato
                virkningsdato = pendata.vedtak.virkningstidspunkt.simpleFormat(),

                //4.1.7 -- $pensjon.vedtak[x].beregning[x]..
                beregning = createBeregningItemList(pendata),

                //4.1.8  $pensjon.vedtak[x].kjoeringsdato -- datoFattetVedtak?
                kjoeringsdato = null,

                //4.1.9
                ukjent = createEkstraTilleggPensjon(pendata),

                //4.1.10 - 4.1.12 $pensjon.vedtak[x].grunnlag
                grunnlag = createGrunnlag(pendata),

                //Na
                mottaker = null,      //ikke i bruk?
                trekkgrunnlag = null,  //ikke i bruk?

                //4.1.13.2 Nei
                begrunnelseAnnen = null,

                //4.1.13.1 -- 4.1.13.2.1 - $pensjon.vedtak[x].avslagbegrunnelse[x].begrunnelse
                avslagbegrunnelse = createAvlsagsBegrunnelseItem(pendata),

                //4.1.14.1 // Ikke i bruk
                delvisstans = null
        )

    }


    /**
     * 4.1.1 - vedtaktype
     *
     * 01 - Old age
     * 02 - Invalidity
     * 03 - Survivors
     * 06 - Early Old age
     *
     * 04 og 05 benyttes ikke
     */
    fun createVedtakTypePensionWithRule(pendata: Pensjonsinformasjon): String {
        //v1sak fra PESYS
        val v1sak = pendata.sakAlder as V1SakAlder

        //Er pensjon før 67 ?
        val pensjonUttakTidligereEnn67 = v1sak.isUttakFor67

        //type fra K_SAK_T
        val type = v1sak.sakType

        val sakType = KSAK.valueOf(type)
        logger.debug("4.1.1         VedtakTypePension")

        return when (sakType) {
            KSAK.ALDER -> {
                when (pensjonUttakTidligereEnn67) {
                    true -> "06"
                    else -> "01"
                }
            }
            KSAK.UFOREP -> "02"
            KSAK.BARNEP, KSAK.GJENLEV -> "03"
        }
    }

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
     * 4.1.2 vedtak
     *
     * [01] Based on residence
     * [02] Based on working
     * [99] Other
     *
     */
    private fun createVedtakGrunnlagPentionWithRule(pendata: Pensjonsinformasjon): String {
        //TODO Det må lages flere regler for UT og for etterlattepensjon

        logger.debug("4.1.2         VedtakGrunnlagPention")

        val sakType = KSAK.valueOf(pendata.sakAlder.sakType)
        logger.debug("              Saktype: $sakType")

        if (sjekkForVilkarsvurderingListeHovedytelseellerAvslag(pendata)) return "99"

        return when (sakType) {
            KSAK.ALDER -> {
                when (isMottarMinstePensjonsniva(pendata)) {
                    true -> "01"
                    false -> "02"
                }
            }
            KSAK.UFOREP -> {
                when (isMottarMinstePensjonsniva(pendata)) {
                    true -> "01"
                    false -> "02"
                }
            }
            KSAK.GJENLEV -> {
                when (isMottarMinstePensjonsniva(pendata)) {
                    true -> "01"
                    false -> "02"
                }
            }
            KSAK.BARNEP -> {
                when (isForeldelos(pendata)) {
                    false -> "99"
                    true -> "99"
                }
            }
        }
    }

    /**
     *  4.1.3.1 (other type)
     */
    private fun createVedtakAnnenTypePentionWithRule(pendata: Pensjonsinformasjon): String? {

        logger.debug("4.1.3.1       VedtakAnnenTypePention")
        if (createVedtakGrunnlagPentionWithRule(pendata) == "99") {

            //TODO: Regler for annen bergeningtekst.
            return "Ytelsen er beregnet etter regler for barnepensjon"

        }
        return null
    }

    /**
     *  4.1.4
     *
     *  HVIS vedtaksresultat er Innvilgelse, OG sakstype IKKE er uføretrygd og kravtype er Førstegangsbehandling Norge/utland ELLER Mellombehandling SÅ skal det hukes for «[01] Award»
     *  HVIS vedtaksresultat er Avslag,  SÅ skal det automatisk hukes for «[02] Rejection»
     *  HVIS kravtype er Revurdering, SÅ skal det hukes for «[03] New calculation / recalculation»
     *  HVIS sakstype er Uføretrygd, OG kravtype er Førstegangsbehandling Norge/utland ELLER Mellombehandling, SÅ skal det hukes for «[04] Provisional or advance payment»
     *  Opphør - må håndteres Se pkt 6.2
     */
    private fun createTypeVedtakPentionWithRule(pendata: Pensjonsinformasjon): String? {
        logger.debug("4.1.4         TypeVedtakPention (vedtak.resultat")

        val sakType = KSAK.valueOf(pendata.sakAlder.sakType)
        val kravGjelder = pendata.vedtak.kravGjelder

        //TODO: finner man vedtaksresultat?
        val vedtaksresultat = hentVilkarsResultatHovedytelse(pendata)
        logger.debug("              vedtaksresultat: $vedtaksresultat")

        val erAvslag = vedtaksresultat == "AVSL"
        val erInnvilgelse = vedtaksresultat == "INNV"

        val erForsteGangBehandlingNorgeUtland = "F_BH_MED_UTL" == kravGjelder
        val erForsteGangBehandlingBosattUtland = "F_BH_BO_UTL" == kravGjelder
        val erMellombehandling = "MELLOMBH" == kravGjelder
        val erRevurdering = kravGjelder == "REVURD"

        if (KSAK.UFOREP != sakType) {
            if (erInnvilgelse && (erForsteGangBehandlingNorgeUtland || erMellombehandling || erForsteGangBehandlingBosattUtland))
                return "01"
        }
        if (erAvslag)
            return "02"

        if (erRevurdering)
            return "03"

        if (KSAK.UFOREP == sakType && (erForsteGangBehandlingNorgeUtland || erMellombehandling))
            return "04"

        if (KSAK.UFOREP == sakType && erForsteGangBehandlingBosattUtland)
            return "01"

        logger.debug("              Ingen verdier funnet. (null)")
        return null
    }

    /**
     *  4.1.7
     */
    fun createBeregningItemList(pendata: Pensjonsinformasjon): List<BeregningItem> {

        val ytelsePerMaaned = pendata.ytelsePerMaanedListe.ytelsePerMaanedListe
                .asSequence().sortedBy { it.fom.toGregorianCalendar() }.toMutableList()


        val resultList = mutableListOf<BeregningItem>()
        val sakType = KSAK.valueOf(pendata.sakAlder.sakType)

        ytelsePerMaaned.forEach {
            resultList.add(createBeregningItem(it, sakType))
        }

        return resultList
    }

    private fun createBeregningItem(ytelsePrMnd: V1YtelsePerMaaned, sakType: KSAK): BeregningItem {
        logger.debug("4.1.7         BeregningItem (Repeterbart)")

        return BeregningItem(
                //4.1.7.1 -- 4.1.7.2
                periode = createBeregningItemPeriode(ytelsePrMnd),

                //4.1.7.3.2 - netAmount -- Nei?
                beloepNetto = null,

                beloepBrutto = BeloepBrutto(
                        //4.1.7.3.1. Gross amount
                        beloep = createBelop(ytelsePrMnd, sakType),

                        //4.1.7.3.3. Gross amount of basic pension
                        ytelseskomponentGrunnpensjon = createYtelseskomponentGrunnpensjon(ytelsePrMnd, sakType),

                        //4.1.7.3.4. Gross amount of supplementary pension
                        ytelseskomponentTilleggspensjon = createYtelseskomponentTilleggspensjon(ytelsePrMnd, sakType),

                        ytelseskomponentAnnen = null
                ),

                //4.1.7.4 Currency automatisk hukes for "NOK" norway krone.
                valuta = "NOK",

                //4.1.7.5              //03 - montly 12/year
                utbetalingshyppighet = "maaned_12_per_aar",

                //4.1.7.6.1     - Nei
                utbetalingshyppighetAnnen = null
        )
    }

    /**
     *  4.1.7.3.1. Gross amount
     */
    private fun createBelop(ytelsePrMnd: V1YtelsePerMaaned, sakType: KSAK): String {
        logger.debug("4.1.7.3.1         Gross amount")
        val belop = ytelsePrMnd.belop

        if (KSAK.UFOREP == sakType) {
            val uforUtOrd = hentYtelseskomponentBelop("UT_ORDINER,UT_TBF,UT_TBS", ytelsePrMnd)
            if (uforUtOrd > belop) {
                return uforUtOrd.toString()
            }
            return belop.toString()
        }
        return belop.toString()
    }

    /**
     *  4.1.7.3.3
     *
     *  Her skal det automatisk vises brutto grunnpensjon for de ulike beregningsperioder  Brutto garantipensjon for alderspensjon beregnet etter kapittel 20.
     */
    private fun createYtelseskomponentGrunnpensjon(ytelsePrMnd: V1YtelsePerMaaned, sakType: KSAK): String? {
        logger.debug("4.1.7.3.3         Grunnpensjon")

        if (KSAK.UFOREP != sakType) {
            return hentYtelseskomponentBelop("GP,GT,ST", ytelsePrMnd).toString()
        }
        return null
    }

    /**
     *  4.1.7.3.4
     *
     *  Her skal det automatisk vises brutto tilleggspensjon for de ulike beregningsperioder  Brutto inntektspensjon for alderspensjon beregnet etter kapittel 20.
     */
    private fun createYtelseskomponentTilleggspensjon(ytelsePrMnd: V1YtelsePerMaaned, sakType: KSAK): String? {
        logger.debug("4.1.7.3.4         Tilleggspensjon")

        if (KSAK.UFOREP != sakType) {
            return hentYtelseskomponentBelop("TP,IP", ytelsePrMnd).toString()
        }
        return null
    }

    /**
     * 4.1.8
     */
    private fun createBeregningItemPeriode(ytelsePrMnd: V1YtelsePerMaaned): Periode {
        logger.debug("4.1.7.1         BeregningItemPeriode")

        var tomstr: String? = null
        var fomstr: String? = null

        val fom = ytelsePrMnd.fom
        if (fom != null)
            fomstr = fom.simpleFormat()

        val tom = ytelsePrMnd.tom
        if (tom != null)
            tomstr = tom.simpleFormat()

        return Periode(
                fom = fomstr,
                tom = tomstr
        )
    }

    /**
     * 4.1.9
     */
    private fun createEkstraTilleggPensjon(pendata: Pensjonsinformasjon): Ukjent? {
        logger.debug("4.1.9         ekstra tilleggpensjon")

        var summer = 0
        pendata.ytelsePerMaanedListe.ytelsePerMaanedListe.forEach {
            summer += hentYtelseskomponentBelop("GJENLEV,TBF,TBS,PP,SKJERMT", it)
        }
        val ukjent = Ukjent(beloepBrutto = BeloepBrutto(ytelseskomponentAnnen = summer.toString()))
        if (summer > 0) {
            return ukjent
        }
        return null
    }

    /**
     * 4.1.10 - 4.1.12
     */
    private fun createGrunnlag(pendata: Pensjonsinformasjon): Grunnlag {

        logger.debug("4.1.10        Grunnlag")

        if (sjekkForVilkarsvurderingListeHovedytelseellerAvslag(pendata)) return Grunnlag()

        return Grunnlag(

                //4.1.10 - Nei
                medlemskap = null,

                //4.1.11
                opptjening = Opptjening(forsikredeAnnen = createOpptjeningForsikredeAnnen(pendata)),

                //4.1.12   $pensjon.vedtak[x].grunnlag.framtidigtrygdetid
                framtidigtrygdetid = createFramtidigtrygdetid(pendata)
        )
    }

    /**
     *  4.1.12. Credited period
     *
     *  [1] Yes
     *  [0] No
     */
    private fun createFramtidigtrygdetid(pendata: Pensjonsinformasjon): String {
        logger.debug("4.1.12        Framtidigtrygdetid")

        return when (KSAK.valueOf(pendata.sakAlder.sakType)) {
            KSAK.ALDER -> "0"
            else -> {
                when (hentVinnendeBergeningsMetode(pendata)) {
                    "FOLKETRYGD" -> "1"

                    else -> "0"
                }
            }
        }
    }

    /**
     *  4.1.11 - OpptjeningForsikredeAnnen
     *
     * [01] - Nei
     * [02] - Delvis
     * [03] - Fullstendig
     *
     */
    private fun createOpptjeningForsikredeAnnen(pendata: Pensjonsinformasjon): String? {
        logger.debug("4.1.11        OpptjeningForsikredeAnnen")

        val sakType = KSAK.valueOf(pendata.sakAlder.sakType)

        val resultatGjenlevendetillegg = pendata.vilkarsvurderingListe?.vilkarsvurderingListe?.get(0)?.resultatGjenlevendetillegg
                ?: ""
        val erUtenGjenlevendetillegg = resultatGjenlevendetillegg == ""
        val erMedGjenlevendetillegg = resultatGjenlevendetillegg != ""
        val vinnendeMetode = hentVinnendeBergeningsMetode(pendata)


        if ((KSAK.ALDER == sakType || KSAK.UFOREP == sakType) && erUtenGjenlevendetillegg)
            return "01"

        if (KSAK.ALDER == sakType && erMedGjenlevendetillegg && vinnendeMetode != "RETT_TIL_GJT")
            return "01"

        if (KSAK.ALDER == sakType && erMedGjenlevendetillegg && "RETT_TIL_GJT" == vinnendeMetode)
            return "02"

        if (KSAK.GJENLEV == sakType || KSAK.BARNEP == sakType) {
            return "03"
        }

        return null
    }

    /**
     * /4.1.13.1 - 4.1.13.2.1
     */
    private fun createAvlsagsBegrunnelseItem(pendata: Pensjonsinformasjon): List<AvslagbegrunnelseItem>? {

        logger.debug("4.1.13        AvlsagsBegrunnelseItem")

        val avslagbegrunnelse = createAvlsagsBegrunnelse(pendata)

        val item = listOf(AvslagbegrunnelseItem(

                //4.1.13.1
                begrunnelse = avslagbegrunnelse,

                //4.1.13.2 Other - Nei
                annenbegrunnelse = null
        ))

        if (avslagbegrunnelse == null)
            return null

        return item
    }

    /**
     *  4.1.13.1 - Rejection reasons
     */
    fun createAvlsagsBegrunnelse(pendata: Pensjonsinformasjon): String? {
        logger.debug("4.1.13.1          AvlsagsBegrunnelse")

        if (pendata.vilkarsvurderingListe == null || pendata.vilkarsvurderingListe.vilkarsvurderingListe == null) {
            return null
        }
        val sakType = KSAK.valueOf(pendata.sakAlder.sakType)

        val avslagVilkarsproving = hentVilkarsResultatHovedytelse(pendata) == "AVSL"

        val harBoddArbeidetUtland = harBoddArbeidetUtland(pendata)
        val erTrygdetidListeTom = pendata.trygdetidListe.trygdetidListe.isEmpty()

        val erLavtTidligUttak = isVilkarsvurderingAvslagHovedytelseSamme("LAVT_TIDLIG_UTTAK", pendata)
        val erUnder62 = isVilkarsvurderingAvslagHovedytelseSamme("UNDER_62", pendata)
        val erIkkeMottattDok = "IKKE_MOTTATT_DOK" == hentVilkarsProvingAvslagHovedYtelse(pendata)

        //UFOREP
        val erForutMedlem = "FORUT_MEDL" == hentVilkarsvurderingUforetrygd(pendata).unntakForutgaendeMedlemskap
        val erHensArbrettTiltak = "HENS_ARBRETT_TILTAK" == hentVilkarsvurderingUforetrygd(pendata).hensiktsmessigArbeidsrettedeTiltak
        val erHensiktmessigBeh = "HENSIKTSMESSIG_BEH" == hentVilkarsvurderingUforetrygd(pendata).hensiktsmessigBehandling
        val erNedsattInntEvne = "NEDSATT_INNT_EVNE" == hentVilkarsvurderingUforetrygd(pendata).nedsattInntektsevne
        val erAlder = "ALDER" == hentVilkarsvurderingUforetrygd(pendata).alder

        //pkt1 og pkt.9
        if ((KSAK.ALDER == sakType || KSAK.BARNEP == sakType || KSAK.GJENLEV == sakType) && harBoddArbeidetUtland && erTrygdetidListeTom && avslagVilkarsproving)
            return "01"

        //pkt.2 og pkt.10
        if ((KSAK.ALDER == sakType || KSAK.BARNEP == sakType || KSAK.GJENLEV == sakType) && harBoddArbeidetUtland && erTrygdeTid(pendata) && avslagVilkarsproving)
            return "02"

        if (KSAK.ALDER == sakType && harBoddArbeidetUtland && erLavtTidligUttak && avslagVilkarsproving)
            return "03"

        if (KSAK.ALDER == sakType && harBoddArbeidetUtland && erUnder62 && avslagVilkarsproving)
            return "06"
        //hentVilkarsvurderingUforetrygd
        if (KSAK.UFOREP == sakType && harBoddArbeidetUtland && erAlder && avslagVilkarsproving)
            return "03"

        if (KSAK.UFOREP == sakType && harBoddArbeidetUtland && (erHensiktmessigBeh || erHensArbrettTiltak) && avslagVilkarsproving)
            return "08"

        if (KSAK.UFOREP == sakType && harBoddArbeidetUtland && erNedsattInntEvne && avslagVilkarsproving)
            return "04"

        if (KSAK.UFOREP == sakType && harBoddArbeidetUtland && erTrygdeTid(pendata) && erForutMedlem && avslagVilkarsproving)
            return "02"
        //pkt.5
        if (KSAK.UFOREP == sakType && harBoddArbeidetUtland && pendata.trygdetidListe.trygdetidListe.isEmpty() && erForutMedlem && avslagVilkarsproving)
            return "01"

        //siste..   pendata.sak.sakType alle..
        if (harBoddArbeidetUtland && erIkkeMottattDok && avslagVilkarsproving)
            return "07"

        logger.debug("              -- Ingen avslagsbegrunnelse")
        return null
    }
}
