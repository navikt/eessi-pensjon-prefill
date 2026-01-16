package no.nav.eessi.pensjon.prefill.sed.vedtak.helper


import no.nav.eessi.pensjon.eux.model.sed.BeloepBrutto
import no.nav.eessi.pensjon.eux.model.sed.BeregningItem
import no.nav.eessi.pensjon.eux.model.sed.Periode
import no.nav.eessi.pensjon.eux.model.sed.Ukjent
import no.nav.eessi.pensjon.prefill.models.YtelseskomponentType.*
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.utils.simpleFormat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PrefillPensjonVedtaksbelop {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPensjonVedtaksbelop::class.java) }

    /**
     *  4.1.7.3.1. Gross amount
     */
    fun createBelop(ytelsePrMnd: P6000MeldingOmVedtakDto.YtelsePerMaaned, sakType: KSAK): String {
        logger.info("4.1.7.3.1         Gross amount")
        val belop = ytelsePrMnd.belop

        if (KSAK.UFOREP == sakType) {
            val uforUtOrd = VedtakPensjonDataHelper.hentYtelseskomponentBelop("UT_ORDINER,UT_TBF,UT_TBS", ytelsePrMnd)
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
     *  Hentet ytelseskomponentType fra YtelseKomponentTypeCode.java (PESYS)
     *      GAP =Garantitillegg
     */
    fun createYtelseskomponentGrunnpensjon(ytelsePrMnd: P6000MeldingOmVedtakDto.YtelsePerMaaned, sakType: KSAK): String? {
        logger.info("4.1.7.3.3         Grunnpensjon")

        if (KSAK.UFOREP != sakType) {
            return VedtakPensjonDataHelper.hentYtelseskomponentBelop(
                "$GAP, $GP, $GAT, $PT, $ST, $MIN_NIVA_TILL_INDV, $MIN_NIVA_TILL_PPAR, $AP_GJT_KAP19", ytelsePrMnd).toString()
        }
        return null
    }

    /**
     *  4.1.7.3.4
     *
     *  Her skal det automatisk vises brutto tilleggspensjon for de ulike beregningsperioder  Brutto inntektspensjon for alderspensjon beregnet etter kapittel 20.
     */
    fun createYtelseskomponentTilleggspensjon(ytelsePrMnd: P6000MeldingOmVedtakDto.YtelsePerMaaned, sakType: KSAK): String? {
        logger.info("4.1.7.3.4         Tilleggspensjon")

        if (KSAK.UFOREP != sakType) {
            return VedtakPensjonDataHelper.hentYtelseskomponentBelop("$TP,$IP", ytelsePrMnd).toString()
        }
        return null
    }

    /**
     * 4.1.9
     */
    fun createEkstraTilleggPensjon(pendata: P6000MeldingOmVedtakDto): Ukjent? {
        logger.info("4.1.9         ekstra tilleggpensjon")

        var summer = 0
        pendata.ytelsePerMaanedListe.forEach {
            summer += VedtakPensjonDataHelper.hentYtelseskomponentBelop("GJENLEV,TBF,TBS,PP,SKJERMT", it)
        }
        val ukjent = Ukjent(beloepBrutto = BeloepBrutto(ytelseskomponentAnnen = summer.toString()))
        if (summer > 0) {
            return ukjent
        }
        return null
    }

    /**
     *  4.1.7
     */
    fun createBeregningItemList(pendata: P6000MeldingOmVedtakDto): List<BeregningItem> {
        logger.info("4.1.7        BeregningItemList")

        val ytelsePerMaaned = pendata.ytelsePerMaanedListe
                .asSequence().sortedBy { it.fom }.toMutableList()

        val sakType = pendata.sakAlder.sakType

        return ytelsePerMaaned.map {
           createBeregningItem(it, sakType)
        }
    }

    /**
     * 4.1.8
     */
    private fun createBeregningItemPeriode(ytelsePrMnd: P6000MeldingOmVedtakDto.YtelsePerMaaned): Periode {
        logger.info("4.1.7.1         BeregningItemPeriode")

        var tomstr: String? = null
        var fomstr: String?

        val fom = ytelsePrMnd.fom
        fomstr = fom.simpleFormat()

        val tom = ytelsePrMnd.tom
        if (tom != null)
            tomstr = tom.simpleFormat()

        return Periode(
                fom = fomstr,
                tom = tomstr
        )
    }

    private fun createBeregningItem(ytelsePrMnd: P6000MeldingOmVedtakDto.YtelsePerMaaned, sakType: KSAK): BeregningItem {
        logger.info("4.1.7         BeregningItem (Repeterbart)")

        return BeregningItem(
                //4.1.7.1 -- 4.1.7.2
                periode = createBeregningItemPeriode(ytelsePrMnd),

                beloepBrutto = BeloepBrutto(
                        //4.1.7.3.1. Gross amount
                        beloep = createBelop(ytelsePrMnd, sakType),

                        //4.1.7.3.3. Gross amount of basic pension
                        ytelseskomponentGrunnpensjon = createYtelseskomponentGrunnpensjon(ytelsePrMnd, sakType),

                        //4.1.7.3.4. Gross amount of supplementary pension
                        ytelseskomponentTilleggspensjon = createYtelseskomponentTilleggspensjon(ytelsePrMnd, sakType),
                ),

                //4.1.7.4 Currency automatisk hukes for "NOK" norway krone.
                valuta = "NOK",

                //4.1.7.5              //03 - montly 12/year
                utbetalingshyppighet = "maaned_12_per_aar"
        )
    }
}