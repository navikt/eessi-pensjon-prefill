package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sakalder.V1SakAlder
import no.nav.pensjon.v1.trygdetid.V1Trygdetid
import no.nav.pensjon.v1.trygdetidliste.V1TrygdetidListe
import no.nav.pensjon.v1.vedtak.V1Vedtak
import no.nav.pensjon.v1.vilkarsvurdering.V1Vilkarsvurdering
import no.nav.pensjon.v1.vilkarsvurderingliste.V1VilkarsvurderingListe
import no.nav.pensjon.v1.vilkarsvurderinguforetrygd.V1VilkarsvurderingUforetrygd
import no.nav.pensjon.v1.ytelsepermaaned.V1YtelsePerMaaned
import no.nav.pensjon.v1.ytelsepermaanedliste.V1YtelsePerMaanedListe
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

object PrefillVedtakTestHelper {

    fun generateFakePensjoninformasjonForKSAK(ksak: String): Pensjonsinformasjon {
        val xmlcal = DatatypeFactory.newInstance().newXMLGregorianCalendar()

        val v1sak = V1SakAlder()
        v1sak.sakType = ksak // "ALDER"
        v1sak.isUttakFor67 = false
        val v1vedtak = V1Vedtak()

        v1vedtak.datoFattetVedtak = xmlcal
        v1vedtak.virkningstidspunkt = xmlcal
        v1vedtak.isBoddArbeidetUtland = true

        val ytelsePerMaanedListe = V1YtelsePerMaanedListe()

        val v1YtelsePerMaaned = V1YtelsePerMaaned()
        v1YtelsePerMaaned.isMottarMinstePensjonsniva = true
        v1YtelsePerMaaned.fom = xmlcal
        v1YtelsePerMaaned.tom = xmlcal
        ytelsePerMaanedListe.ytelsePerMaanedListe.add(v1YtelsePerMaaned)

        val vilvirdUtfor = V1VilkarsvurderingUforetrygd()
        vilvirdUtfor.alder = "ALDER"
        vilvirdUtfor.nedsattInntektsevne = "SANN"

        val vilvurder = V1Vilkarsvurdering()
        vilvurder.avslagHovedytelse = "ALDER_PPP"
        vilvurder.vilkarsvurderingUforetrygd = vilvirdUtfor

        val vilkarsvurderingListe = V1VilkarsvurderingListe()
        vilkarsvurderingListe.vilkarsvurderingListe.add(vilvurder)

        val trygdetidListe = createTrygdelisteTid()

        val peninfo = Pensjonsinformasjon()
        peninfo.sakAlder = v1sak
        peninfo.vedtak = v1vedtak

        peninfo.ytelsePerMaanedListe = ytelsePerMaanedListe
        peninfo.vilkarsvurderingListe = vilkarsvurderingListe
        peninfo.trygdetidListe = trygdetidListe

        return peninfo
    }

    fun createTrygdelisteTid(): V1TrygdetidListe {
        val ttid1 = V1Trygdetid()
        ttid1.fom = convertToXMLcal(LocalDate.now().minusDays(25))
        ttid1.tom = convertToXMLcal(LocalDate.now().minusDays(20))

        val ttid2 = V1Trygdetid()
        ttid2.fom = convertToXMLcal(LocalDate.now().minusDays(10))
        ttid2.tom = convertToXMLcal(LocalDate.now().minusDays(5))

        val ttid3 = V1Trygdetid()
        ttid3.fom = convertToXMLcal(LocalDate.now().minusDays(0))
        ttid3.tom = convertToXMLcal(LocalDate.now().plusDays(5))

        val trygdetidListe = V1TrygdetidListe()
        trygdetidListe.trygdetidListe.add(ttid1)
        trygdetidListe.trygdetidListe.add(ttid2)
        trygdetidListe.trygdetidListe.add(ttid3)
        return trygdetidListe
    }

    fun convertToXMLcal(time: LocalDate): XMLGregorianCalendar {
        val gcal = GregorianCalendar()
        gcal.setTime(Date.from(time.atStartOfDay(ZoneId.systemDefault()).toInstant()))
        val xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
        return xgcal
    }
}
