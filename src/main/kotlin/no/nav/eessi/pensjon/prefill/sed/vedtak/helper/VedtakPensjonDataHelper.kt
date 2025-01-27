package no.nav.eessi.pensjon.prefill.sed.vedtak.helper

import no.nav.pensjon.v1.avdod.V1Avdod
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.trygdetidliste.V1TrygdetidListe
import no.nav.pensjon.v1.vilkarsvurdering.V1Vilkarsvurdering
import no.nav.pensjon.v1.vilkarsvurderingliste.V1VilkarsvurderingListe
import no.nav.pensjon.v1.vilkarsvurderinguforetrygd.V1VilkarsvurderingUforetrygd
import no.nav.pensjon.v1.ytelsepermaaned.V1YtelsePerMaaned
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object VedtakPensjonDataHelper {

    private val logger: Logger by lazy { LoggerFactory.getLogger(VedtakPensjonDataHelper::class.java) }

    fun harBoddArbeidetUtland(pendata: Pensjonsinformasjon): Boolean {
        require(pendata.vedtak != null) { "Vedtak er null" }
        return pendata.vedtak.isBoddArbeidetUtland || harAvdodBoddArbeidetUtland(pendata)
    }

    private fun harAvdodBoddArbeidetUtland(pendata: Pensjonsinformasjon): Boolean {
        val avdod: V1Avdod = pendata.avdod ?: return false

        return (!avdod.avdod.isNullOrBlank() && avdod.isAvdodBoddArbeidetUtland == true) ||
                (avdod.isAvdodMorBoddArbeidetUtland == true || avdod.isAvdodFarBoddArbeidetUtland == true)
    }

    fun isVilkarsvurderingAvslagHovedytelseSamme(key: String, pendata: Pensjonsinformasjon): Boolean {
        return key == hentVilkarsProvingAvslagHovedYtelse(pendata)
    }

    fun erTrygdeTid(pendata: Pensjonsinformasjon, storreEnn: Int = 30, mindreEnn: Int = 360): Boolean {
        require(pendata.trygdetidListe?.trygdetidListe != null) { "trygdetidListe er Null" }
        val trygdeListe = pendata.trygdetidListe
        val days = summerTrygdeTid(trygdeListe)

        return days in (storreEnn + 1) until mindreEnn
    }

    fun summerTrygdeTid(trygdeListe: V1TrygdetidListe): Int {
        require(trygdeListe.trygdetidListe != null) { "trygdetidListe er Null" }
        val daylist = trygdeListe.trygdetidListe.map {
            val fom = it.fom.toGregorianCalendar().time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val tom = it.tom.toGregorianCalendar().time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val nrdays = ChronoUnit.DAYS.between(fom, tom)
            logger.debug("              SummerTrygdeTid: $nrdays  fom: $fom  tom: $tom ")
            nrdays.toInt()
        }
        return daylist.sumOf { it }
            .also { days -> logger.debug("              Total SummerTrygdeTid: $days ") }
    }

    fun hentYtelseskomponentBelop(keys: String, ytelse: V1YtelsePerMaaned): Int {
        val keylist = keys.split(",")
        var summer = 0
        keylist.forEach { keyword ->
            ytelse.ytelseskomponentListe.forEach { it2 ->
                if (keyword.trim() == it2.ytelsesKomponentType) {
                    summer += it2.belopTilUtbetaling
                }
            }
        }
        return summer
    }

    fun hentGrunnPersjon(pendata: Pensjonsinformasjon?): Boolean {
        return pendata?.trygdeavtale?.isErArt10BruktGP ?: false
    }

    fun hentTilleggsPensjon(pendata: Pensjonsinformasjon?): Boolean {
        return pendata?.trygdeavtale?.isErArt10BruktTP ?: false
    }

    fun hentVilkarsvurderingUforetrygd(pendata: Pensjonsinformasjon): V1VilkarsvurderingUforetrygd {
        return hentV1Vilkarsvurdering(pendata)?.vilkarsvurderingUforetrygd ?: return V1VilkarsvurderingUforetrygd()
    }

    //         Kodeverk K_RESULT_BEGR 2017
    fun hentVilkarsProvingAvslagHovedYtelse(pendata: Pensjonsinformasjon): String {
        return hentV1Vilkarsvurdering(pendata)?.avslagHovedytelse ?: return ""
    }

    private fun hentV1Vilkarsvurdering(pendata: Pensjonsinformasjon): V1Vilkarsvurdering? {
        val v1VilkarsvurderingListe: V1VilkarsvurderingListe =
            pendata.vilkarsvurderingListe ?: V1VilkarsvurderingListe()
        return v1VilkarsvurderingListe.vilkarsvurderingListe?.getOrElse(0) { V1Vilkarsvurdering() }
    }

    fun hentVilkarsResultatHovedytelse(pendata: Pensjonsinformasjon): String {
        return hentV1Vilkarsvurdering(pendata)?.resultatHovedytelse ?: return "" // UNDER_62 -- LAVT_TIDLIG_UTTAK osv..
    }

    fun hentVinnendeBergeningsMetode(pendata: Pensjonsinformasjon): String {
        return hentSisteYtelsePerMaaned(pendata).vinnendeBeregningsmetode.also {
            logger.debug(" +            hentVinnendeBergeningsMetode: $it")
        }
    }

    fun hentYtelseBelop(pendata: Pensjonsinformasjon): String {
        logger.debug(" +            hentYtelseBelop")
        return hentSisteYtelsePerMaaned(pendata).belop.toString()
    }

    fun isMottarMinstePensjonsniva(pendata: Pensjonsinformasjon): Boolean {
        logger.debug(" +            isMottarMinstePensjonsniva")
        return hentSisteYtelsePerMaaned(pendata).isMottarMinstePensjonsniva
    }

    fun hentSisteYtelsePerMaaned(pendata: Pensjonsinformasjon): V1YtelsePerMaaned {
        val ytelseprmnd = pendata.ytelsePerMaanedListe
        val liste = ytelseprmnd.ytelsePerMaanedListe as List<V1YtelsePerMaaned>
        return liste.maxByOrNull { it.fom.toGregorianCalendar() }!!
    }
}
