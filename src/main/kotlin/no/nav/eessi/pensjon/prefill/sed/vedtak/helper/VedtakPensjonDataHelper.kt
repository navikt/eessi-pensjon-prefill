package no.nav.eessi.pensjon.prefill.sed.vedtak.helper

import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.models.pensjon.YtelsePerMndBase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit

object VedtakPensjonDataHelper {

    private val logger: Logger by lazy { LoggerFactory.getLogger(VedtakPensjonDataHelper::class.java) }

    fun harBoddArbeidetUtland(pesysPrefillData: P6000MeldingOmVedtakDto): Boolean {
        return pesysPrefillData.vedtak.boddArbeidetUtland?: false || harAvdodBoddArbeidetUtland(pesysPrefillData)
    }

    private fun harAvdodBoddArbeidetUtland(pesysPrefillData: P6000MeldingOmVedtakDto): Boolean {
        val avdod = pesysPrefillData.avdod ?: return false

        return (!avdod.avdod.isNullOrBlank() && avdod.avdodBoddArbeidetUtland == true) ||
                (avdod.avdodMorBoddArbeidetUtland == true || avdod.avdodFarBoddArbeidetUtland == true)
    }

    fun isVilkarsvurderingAvslagHovedytelseSamme(key: String, pendata: P6000MeldingOmVedtakDto): Boolean {
        return key == hentVilkarsProvingAvslagHovedYtelse(pendata)
    }

    fun erTrygdeTid(pendata: P6000MeldingOmVedtakDto, storreEnn: Int = 30, mindreEnn: Int = 360): Boolean {
        if (pendata.trygdetid.isEmpty()) { logger.warn("trygdetidListe er tom"); return false }
        val days = summerTrygdeTid(pendata.trygdetid)

        return days in (storreEnn + 1) until mindreEnn
    }

    fun summerTrygdeTid(trygdeListe: List<P6000MeldingOmVedtakDto.Trygdetid>): Int {
        val daylist = trygdeListe.map {
            val nrdays = ChronoUnit.DAYS.between(it.fom, it.tom)
            logger.debug("              SummerTrygdeTid: $nrdays  fom: ${it.fom}  tom: ${it.tom} ")
            nrdays.toInt()
        }
        return daylist.sumOf { it }
            .also { days -> logger.debug("              Total SummerTrygdeTid: $days ") }
    }

    fun hentYtelseskomponentBelop(keys: String, ytelse: YtelsePerMndBase): Int {
        val keylist = keys.split(",")
        var summer = 0
        keylist.forEach { keyword ->
            ytelse.ytelseskomponent?.forEach { it2 ->
                if (keyword.trim() == it2.ytelsesKomponentType) {
                    summer += it2.belopTilUtbetaling
                }
            }
        }
        return summer
    }

    fun hentGrunnPersjon(pendata: P6000MeldingOmVedtakDto): Boolean {
        return pendata.trygdeavtale?.erArt10BruktGP ?: false
    }

    fun hentTilleggsPensjon(pendata: P6000MeldingOmVedtakDto): Boolean {
        return pendata.trygdeavtale?.erArt10BruktTP ?: false
    }

    fun hentVilkarsvurderingUforetrygd(pendata: P6000MeldingOmVedtakDto): P6000MeldingOmVedtakDto.VilkarsvurderingUforetrygd? {
        return hentV1Vilkarsvurdering(pendata)?.vilkarsvurderingUforetrygd
    }

    //         Kodeverk K_RESULT_BEGR 2017
    fun hentVilkarsProvingAvslagHovedYtelse(pendata: P6000MeldingOmVedtakDto): String {
        return hentV1Vilkarsvurdering(pendata)?.avslagHovedytelse ?: return ""
    }

    private fun hentV1Vilkarsvurdering(pendata: P6000MeldingOmVedtakDto): P6000MeldingOmVedtakDto.Vilkarsvurdering? {
        return pendata.vilkarsvurdering.getOrNull(0)
    }

    fun hentVilkarsResultatHovedytelse(pendata: P6000MeldingOmVedtakDto): String {
        return hentV1Vilkarsvurdering(pendata)?.resultatHovedytelse ?: return "" // UNDER_62 -- LAVT_TIDLIG_UTTAK osv..
    }

    fun hentVinnendeBergeningsMetode(pendata: P6000MeldingOmVedtakDto): String? {
        return hentSisteYtelsePerMaaned(pendata)?.vinnendeBeregningsmetode
    }

    fun hentYtelseBelop(pendata: P6000MeldingOmVedtakDto): String? {
        logger.info(" +            hentYtelseBelop")
        return hentSisteYtelsePerMaaned(pendata)?.belop?.toString()
    }

    fun isMottarMinstePensjonsniva(pendata: P6000MeldingOmVedtakDto): Boolean {
        logger.info(" +            isMottarMinstePensjonsniva")
        return hentSisteYtelsePerMaaned(pendata)?.mottarMinstePensjonsniva ?: false
    }

    fun hentSisteYtelsePerMaaned(pendata: P6000MeldingOmVedtakDto): P6000MeldingOmVedtakDto.YtelsePerMaaned? {
        return pendata.ytelsePerMaaned.maxByOrNull { it.fom }
    }
}
