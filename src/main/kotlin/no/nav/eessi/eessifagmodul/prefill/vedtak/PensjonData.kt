package no.nav.eessi.eessifagmodul.prefill.vedtak

import com.google.common.base.Preconditions
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.trygdetidliste.V1TrygdetidListe
import no.nav.pensjon.v1.vilkarsvurderinguforetrygd.V1VilkarsvurderingUforetrygd
import no.nav.pensjon.v1.ytelsepermaaned.V1YtelsePerMaaned
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.time.temporal.ChronoUnit

abstract class PensjonData {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PensjonData::class.java) }

    //K_SAK_T Kodeverk fra PESYS
    enum class KSAK {
        ALDER,
        UFOREP,
        GJENLEV,
        BARNEP;
    }

    init {
        logger.debug("PensjonData")
    }

    fun harBoddArbeidetUtland(pendata: Pensjonsinformasjon): Boolean {
        Preconditions.checkArgument(pendata.vedtak != null, "Vedtak er null")
        return pendata.vedtak.isBoddArbeidetUtland
    }

    fun isVilkarsvurderingAvslagHovedytelseSamme(key: String, pendata: Pensjonsinformasjon): Boolean {
        return key == hentVilkarsProvingAvslagHovedYtelse(pendata)
    }

    //TODO - summere opp i ant. dager . trygdetidListe.fom - tom.
    fun erTrygdeTid(pendata: Pensjonsinformasjon, storreEnn: Int=30, mindreEnn: Int=360): Boolean {
        Preconditions.checkArgument(pendata.trygdetidListe != null, "trygdetidListe er Null")
        Preconditions.checkArgument(pendata.trygdetidListe.trygdetidListe != null, "trygdetidListe er Null")
        val trygdeListe = pendata.trygdetidListe
        val days = summerTrygdeTid(trygdeListe)
        //trygdetid viser trygdetid 30 > < 360 dager
        //println("$storreEnn > $days && $days < $mindreEnn")
        //days =  70 -> 30 > 70 && 70 < 360   - true
        //days =  15 -> 30 > 15 && 15 < 360   - false
        //days = 500 -> 30 > 500 && 500 < 360 - false
        return days in (storreEnn + 1)..(mindreEnn - 1)
    }

    fun summerTrygdeTid(trygdeListe: V1TrygdetidListe): Int {
        Preconditions.checkArgument(trygdeListe.trygdetidListe != null, "trygdetidListe er Null")
        val daylist = mutableListOf<Int>()
        trygdeListe.trygdetidListe.forEach {
            val fom = it.fom.toGregorianCalendar().time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val tom = it.tom.toGregorianCalendar().time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val nrdays = ChronoUnit.DAYS.between(fom,    tom)
            logger.debug("              SummerTrygdeTid: $nrdays  fom: $fom  tom: $tom ")
            daylist.add(nrdays.toInt())
        }
        var days = 0
        daylist.forEach {
            days += it
        }
        logger.debug("              Total SummerTrygdeTid: $days ")
        return days
    }

    fun hentYtelseskomponentBelop(keys: String, ytelse: V1YtelsePerMaaned) : Int {
        val keylist = keys.split(",")
        var summer = 0
        keylist.forEach { keyword ->
            ytelse.ytelseskomponentListe.forEach { it2 ->
                //logger.debug("keyword: $keyword ==> type: ${it2.ytelsesKomponentType}")
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
        return pendata?.trygdeavtale?.isErArt10BruktTP?: false
    }

    fun hentVilkarsvurderingUforetrygd(pendata: Pensjonsinformasjon): V1VilkarsvurderingUforetrygd {
        val v1VilkarsvurderingListe = pendata.vilkarsvurderingListe
        return v1VilkarsvurderingListe.vilkarsvurderingListe?.get(0)?.vilkarsvurderingUforetrygd ?: return V1VilkarsvurderingUforetrygd()//
    }

    //         Kodeverk K_RESULT_BEGR 2017
    fun hentVilkarsProvingAvslagHovedYtelse(pendata: Pensjonsinformasjon): String {
        val v1VilkarsvurderingListe = pendata.vilkarsvurderingListe
        return v1VilkarsvurderingListe.vilkarsvurderingListe?.get(0)?.avslagHovedytelse ?: return "" // UNDER_62 -- LAVT_TIDLIG_UTTAK osv..
    }

    fun hentVilkarsResultatHovedytelse(pendata: Pensjonsinformasjon): String {
        val v1VilkarsvurderingListe = pendata.vilkarsvurderingListe
        return v1VilkarsvurderingListe.vilkarsvurderingListe?.get(0)?.resultatHovedytelse ?: return "" // UNDER_62 -- LAVT_TIDLIG_UTTAK osv..
    }

    fun isForeldelos(pendata: Pensjonsinformasjon): Boolean {
        val avdodpinfar : String? = pendata.avdod.avdodFar ?: "INGEN"
        val avdodpinmor : String? = pendata.avdod.avdodMor ?: "INGEN"
        if (avdodpinfar != "INGEN" && avdodpinmor != "INGEN") {
            return true
        }
        return false
    }

    fun hentVinnendeBergeningsMetode(pendata: Pensjonsinformasjon): String {
        logger.debug(" +            hentVinnendeBergeningsMetode")
        return hentSisteYtelsePerMaaned(pendata).vinnendeBeregningsmetode
    }

    fun hentVurdertBeregningsmetodeNordisk(pendata: Pensjonsinformasjon): Boolean {
        logger.debug(" +            hentVurdertBeregningsmetodeNordisk")
        return hentSisteYtelsePerMaaned(pendata).isVurdertBeregningsmetodeNordisk
    }

    fun hentYtelseBelop(pendata: Pensjonsinformasjon): String {
        logger.debug(" +            hentYtelseBelop")
        return hentSisteYtelsePerMaaned(pendata).belop.toString()
    }

    fun isMottarMinstePensjonsniva(pendata: Pensjonsinformasjon): Boolean {
        logger.debug(" +            isMottarMinstePensjonsniva")
        return hentSisteYtelsePerMaaned(pendata).isMottarMinstePensjonsniva
    }

    fun hentSisteYtelsePerMaaned(pendata: Pensjonsinformasjon): V1YtelsePerMaaned{
        val ytelseprmnd = pendata.ytelsePerMaanedListe
        val liste = ytelseprmnd.ytelsePerMaanedListe as List<V1YtelsePerMaaned>
        return liste.asSequence().sortedBy{ it.fom.toGregorianCalendar() }.toList()[liste.size-1]
    }



}