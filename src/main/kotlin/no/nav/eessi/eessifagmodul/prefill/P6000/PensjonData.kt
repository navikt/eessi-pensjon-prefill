package no.nav.eessi.eessifagmodul.prefill.P6000

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

    //K_KRAV_VELG_T Kodeverk fra PESYS
    enum class KKRAV {
        AVDOD_MOR,
        AVDOD_FAR,
        FORELDRELOS,
        MIL_INV,
        MIL_GJENLEV,
        MIL_BARNEP,
        SIVIL_INV,
        SIVIL_GJENLEV,
        SIVIL_BARNEP,
        FORELOPIG,
        VARIG,
        UP,
        EP,
        BP,
        NSB;
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
        return days > storreEnn && days < mindreEnn
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
        var days: Int = 0
        daylist.forEach {
            days += it
        }
        logger.debug("              Total SummerTrygdeTid: $days ")
        return days
    }

    fun hentYtelseskomponentBelop(keys: String, ytelse: V1YtelsePerMaaned) : Int {
        val keylist = keys.split(",")
        var summer = 0
        keylist.forEach { val keyword = it
            ytelse.ytelseskomponentListe.forEach {
                if (keyword.trim() == it.ytelsesKomponentType) {
                    summer += it.belopTilUtbetaling
                }
            }
        }
        return summer
    }

    fun hentVinnendeBergeningsMetode(pendata: Pensjonsinformasjon): String {
        pendata.ytelsePerMaanedListe.ytelsePerMaanedListe.forEach {
            return it.vinnendeBeregningsmetode
        }
        return ""
    }

    fun hentGrunnPerson(pendata: Pensjonsinformasjon?): Boolean {
        return pendata?.trygdeavtale?.isErArt10BruktGP ?: false
    }

    fun hentTilleggsPensjon(pendata: Pensjonsinformasjon?): Boolean {
        return pendata?.trygdeavtale?.isErArt10BruktTP?: false
    }


    fun hentVurdertBeregningsmetodeNordisk(pendata: Pensjonsinformasjon): Boolean {
        pendata.ytelsePerMaanedListe.ytelsePerMaanedListe.forEach {
            return it.isVurdertBeregningsmetodeNordisk
        }
        return false
    }


    fun hentYtelseBelop(pendata: Pensjonsinformasjon): String {
        pendata.ytelsePerMaanedListe.ytelsePerMaanedListe.forEach {
            return it.belop.toString()
        }
        return "0"
    }


    fun hentVilkarsvurderingUforetrygd(pendata: Pensjonsinformasjon): V1VilkarsvurderingUforetrygd {
        val v1VilkarsvurderingListe = pendata?.vilkarsvurderingListe ?: return V1VilkarsvurderingUforetrygd()
        v1VilkarsvurderingListe.vilkarsvurderingListe.forEach {
            return it?.vilkarsvurderingUforetrygd ?: return V1VilkarsvurderingUforetrygd()//
        }
        return V1VilkarsvurderingUforetrygd()
    }

    //         Kodeverk K_RESULT_BEGR 2017
    fun hentVilkarsProvingAvslagHovedYtelse(pendata: Pensjonsinformasjon): String {
        val v1VilkarsvurderingListe = pendata?.vilkarsvurderingListe ?: return ""
        v1VilkarsvurderingListe.vilkarsvurderingListe.forEach {
            return it?.avslagHovedytelse ?: return "" // UNDER_62 -- LAVT_TIDLIG_UTTAK osv..
        }
        return ""
    }

    fun hentVilkarsResultatHovedytelse(pendata: Pensjonsinformasjon): String {
        val v1VilkarsvurderingListe = pendata?.vilkarsvurderingListe ?: return ""
        v1VilkarsvurderingListe.vilkarsvurderingListe.forEach {
            return it.resultatHovedytelse // UNDER_62 -- LAVT_TIDLIG_UTTAK osv..
        }
        return ""
    }

    fun isForeldelos(pendata: Pensjonsinformasjon): Boolean {
        val avdodpinfar : String? = pendata.avdod.avdodFar ?: "INGEN"
        val avdodpinmor : String? = pendata.avdod.avdodMor ?: "INGEN"
        if (avdodpinfar != "INGEN" && avdodpinmor != "INGEN") {
            return true
        }
        return false
    }

    //hjelpefunkjson for isMottarMinstePensjonsniva
    //Uføretrygd og beregnet ytelse er på minstenivå (minsteytelse)
    fun isMottarMinstePensjonsniva(pendata: Pensjonsinformasjon): Boolean {
        logger.debug(" +            isMottarMinstePensjonsniva")

        val ytelseprmnd = pendata.ytelsePerMaanedListe
        val liste = ytelseprmnd.ytelsePerMaanedListe

        liste.forEach {
            return it.isMottarMinstePensjonsniva
        }
        return false
    }


}