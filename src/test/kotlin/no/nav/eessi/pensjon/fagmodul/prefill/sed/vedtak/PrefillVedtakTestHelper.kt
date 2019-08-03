package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.pensjon.services.pensjonsinformasjon.RequestBuilder
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
import org.mockito.ArgumentMatchers
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

object PrefillVedtakTestHelper {

    fun vedtakDataFromPENFraFil(responseXMLfilename: String): VedtakDataFromPEN {
        val resource = ResourceUtils.getFile("classpath:pensjonsinformasjon/vedtak/$responseXMLfilename").readText()
        val readXMLresponse = ResponseEntity(resource, HttpStatus.OK)

        val mockRestTemplate = mock<RestTemplate>()
        whenever(mockRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java)))
                .thenReturn(readXMLresponse)

        return VedtakDataFromPEN(
                PensjonsinformasjonHjelper(
                        PensjonsinformasjonService(mockRestTemplate, RequestBuilder())))
    }

    fun generatePrefillData(subtractYear: Int, sedId: String, prefill: PrefillDataModel): PrefillDataModel {
        prefill.apply {
            rinaSubject = "Pensjon"
            sed = SED(sedId)
            penSaksnummer = "12345"
            vedtakId = "12312312"
            buc = "P_BUC_99"
            aktoerID = "123456789"
            personNr = generateRandomFnr(subtractYear)
            institution = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        }
        return prefill
    }

    val eessiInformasjon = EessiInformasjon(
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO",
            institutionGate = "Postboks 6600 Etterstad TEST",
            institutionBy = "Oslo",
            institutionPostnr = "0607",
            institutionLand = "NO"
    )

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

    private fun generateRandomFnr(yearsToSubtract: Int): String {
        val fnrdate = LocalDate.now().minusYears(yearsToSubtract.toLong())
        val y = fnrdate.year.toString()
        val day = fixDigits(fnrdate.dayOfMonth.toString())
        val month = fixDigits(fnrdate.month.value.toString())
        val fixedyear = y.substring(2, y.length)
        val fnr = day + month + fixedyear + 43352
        return fnr
    }

    private fun fixDigits(str: String): String {
        if (str.length == 1) {
            return "0$str"
        }
        return str
    }
}
