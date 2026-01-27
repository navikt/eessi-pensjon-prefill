package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.prefill.models.YtelseskomponentType
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

//TODO: Laget som en enkel helper for generering av xml/java objekter for testing av bl.a. yteleser
object PensjonsInformasjonHelper {

//    fun createYtelseskomponent(
//        type: YtelseskomponentType = YtelseskomponentType.GAP,
//        belopTilUtbetaling: Int? = null,
//        belopUtenAvkorting: Int? = null,
//    ) = V1Ytelseskomponent().apply {
//        this.ytelsesKomponentType = type.name
//        this.belopTilUtbetaling = belopTilUtbetaling ?: 0
//        this.belopUtenAvkorting = belopUtenAvkorting ?: 0
//    }

//    fun createYtelsePerMaaned(
//        mottarMinstePensjonsniva: Boolean = false,
//        ytelseskomponenter: List<V1Ytelseskomponent>? = null,
//        belop: Int? = null,
//        fomDate: XMLGregorianCalendar? = null,
//        tomDate: XMLGregorianCalendar? = null,
//        belopUtenAvkorting: Int? = null
//    ) = V1YtelsePerMaaned().apply {
//        this.isMottarMinstePensjonsniva = mottarMinstePensjonsniva
//        this.ytelseskomponentListe.addAll(ytelseskomponenter ?: emptyList())
//        this.fom = fomDate ?: dummyDate()
//        this.tom = tomDate ?: dummyDate()
//        this.belop = belop ?: 0
//        this.belopUtenAvkorting = belopUtenAvkorting ?: 0
//    }

//    fun createKravHistorikk(
//        kravArsak: String? = null,
//        kravType: String? = null,
//        mottattDato: XMLGregorianCalendar? = null,
//        virkningstidspunkt: XMLGregorianCalendar? = null,
//        status: Sakstatus? = null,
//    ) = V1KravHistorikk().apply {
//        this.kravArsak = kravArsak ?: ""
//        this.kravType = kravType ?: ""
//        this.mottattDato = mottattDato ?: dummyDate()
//        this.virkningstidspunkt = virkningstidspunkt ?: dummyDate()
//        this.status = status?.name ?: ""
//    }

//    fun createSak(
//        kravHistorikk: V1KravHistorikk? = null,
//        ytelsePerMaaned: V1YtelsePerMaaned? = null,
//        sakType : String? = null,
//        status: Sakstatus = Sakstatus.TIL_BEHANDLING,
//        forsteVirkningstidspunkt: XMLGregorianCalendar? = null
//    ) = V1Sak().apply {
//        this.status  = status.name
//        this.sakType = sakType ?: "GJENLEV"
//        this.forsteVirkningstidspunkt = forsteVirkningstidspunkt ?: dummyDate()
//        this.ytelsePerMaanedListe = V1YtelsePerMaanedListe().apply { ytelsePerMaanedListe.add(ytelsePerMaaned) }
//        this.kravHistorikkListe = V1KravHistorikkListe().apply { kravHistorikkListe.addAll(mutableListOf(kravHistorikk)) }
//    }

    fun dummyDate(days : Int = 0): XMLGregorianCalendar {
        val calendar = GregorianCalendar()
        calendar.add(GregorianCalendar.DAY_OF_MONTH, days)
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar)
    }
}