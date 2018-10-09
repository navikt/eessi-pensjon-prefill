package no.nav.eessi.eessifagmodul.prefill

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.createSED
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.RequestBuilder
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.trygdetid.V1Trygdetid
import no.nav.pensjon.v1.trygdetidliste.V1TrygdetidListe
import no.nav.pensjon.v1.vedtak.V1Vedtak
import no.nav.pensjon.v1.vilkarsvurdering.V1Vilkarsvurdering
import no.nav.pensjon.v1.vilkarsvurderingliste.V1VilkarsvurderingListe
import no.nav.pensjon.v1.vilkarsvurderinguforetrygd.V1VilkarsvurderingUforetrygd
import no.nav.pensjon.v1.ytelsepermaaned.V1YtelsePerMaaned
import no.nav.pensjon.v1.ytelsepermaanedliste.V1YtelsePerMaanedListe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PrefillP6000PensionDataFromPESYSTest {


    @Mock
    lateinit var pensjonsinformasjonService: PensjonsinformasjonService

    @Mock
    lateinit var pensjonsinformasjonRestTemplate: RestTemplate

    lateinit var dataFromPESYS: PrefillP6000PensionDataFromPESYS
    lateinit var  prefill: PrefillDataModel

    @Before
    fun setup() {
        prefill = PrefillDataModel()
        dataFromPESYS = PrefillP6000PensionDataFromPESYS(pensjonsinformasjonService)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test for pensjon feiler mangler vedtakid`() {
        prefill = generatePrefillData(68, "P6000")
        prefill.vedtakId = ""
        dataFromPESYS.prefill(prefill)

    }

    fun readXMLresponse(file: String): ResponseEntity<String> {
        val resource = ResourceUtils.getFile("classpath:pensjonsinformasjon/$file").readText()
        return ResponseEntity(resource, HttpStatus.OK)
    }

    @Test
    fun `Grunnlag for vedtak alderpensjon utland old age use P6000-APUtland-101`() {
        prefill = generatePrefillData(68, "P6000")

        val pensjonsinformasjonService1 = PensjonsinformasjonService(pensjonsinformasjonRestTemplate, RequestBuilder())
        val dataFromPESYS1 = PrefillP6000PensionDataFromPESYS(pensjonsinformasjonService1)
        whenever(pensjonsinformasjonRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))).thenReturn(readXMLresponse("P6000-APUtland-301.xml"))

        val result = dataFromPESYS1.prefill(prefill)

        assertNotNull(result)

        assertNotNull(result)
        val vedtak = result.vedtak
        assertNotNull(vedtak)
        vedtak?.forEach {
            assertEquals("2017-05-01" , it.virkningsdato)
            assertEquals("01", it.type)
            assertEquals("02", it.basertPaa)

            val bergen = it?.beregning?.get(0)
            assertEquals("2017-10-05", bergen?.periode?.fom)
            assertEquals(null, bergen?.periode?.tom)

            val avslagbrg = it.avslagbegrunnelse?.get(0)
            assertEquals(null, avslagbrg?.begrunnelse)

        }

        val sed = prefill.sed
        sed.pensjon = result

        val json = sed.toJson()
        println("P6000 json\n:$json")

    }

    @Test
    fun `Grunnlag for ytelse alderpensjon uforepen`() {
        prefill = generatePrefillData(66, "P6000")

        val pensjonsinformasjonService1 = PensjonsinformasjonService(pensjonsinformasjonRestTemplate, RequestBuilder())
        val dataFromPESYS2 = PrefillP6000PensionDataFromPESYS(pensjonsinformasjonService1)
        whenever(pensjonsinformasjonRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))).thenReturn(readXMLresponse("P6000-UT-201.xml"))

        val result = dataFromPESYS2.prefill(prefill)

        assertNotNull(result)
        val vedtak = result.vedtak
        assertNotNull(vedtak)
        vedtak?.forEach {
            assertEquals("02", it.type, "Type")
            assertEquals("2017-04-11" , it.virkningsdato)
            assertEquals("02", it.basertPaa, "BasertPaa")
        }

    }

    @Test
    fun `summerTrygdeTid med 10 dager totalt`() {


        var ttid1 = V1Trygdetid()
        ttid1.fom = convertToXMLcal(LocalDate.now().minusDays(50))
        ttid1.tom = convertToXMLcal(LocalDate.now().minusDays(40))


        var trygdetidListe = V1TrygdetidListe()
        trygdetidListe.trygdetidListe.add(ttid1)

        val result = dataFromPESYS.summerTrygdeTid(trygdetidListe)
        println("resultat: $result")
        assertEquals(10, result)
    }

    @Test
    fun `summerTrygdeTid med 70 dager totalt`() {
        var ttid1 = V1Trygdetid()
        ttid1.fom = convertToXMLcal(LocalDate.now().minusDays(170))
        ttid1.tom = convertToXMLcal(LocalDate.now().minusDays(100))

        var trygdetidListe = V1TrygdetidListe()
        trygdetidListe.trygdetidListe.add(ttid1)

        val result = dataFromPESYS.summerTrygdeTid(trygdetidListe)
        assertEquals(70, result)

        val pendata = Pensjonsinformasjon()
        pendata.trygdetidListe = trygdetidListe
        val bolresult = dataFromPESYS.erTrygdeTid(pendata)
        //bod i utland mindre en mer en 30 mindre en 360?
        assertEquals(true, bolresult)
    }

    @Test
    fun `summerTrygdeTid med 15 dager totalt`() {

        val ttid1 = V1Trygdetid()
        ttid1.fom = convertToXMLcal(LocalDate.now().minusDays(25))
        ttid1.tom = convertToXMLcal(LocalDate.now().minusDays(20))

        val ttid2 = V1Trygdetid()
        ttid2.fom = convertToXMLcal(LocalDate.now().minusDays(10))
        ttid2.tom = convertToXMLcal(LocalDate.now().minusDays(5))

        val ttid3 = V1Trygdetid()
        ttid3.fom = convertToXMLcal(LocalDate.now().minusDays(0))
        ttid3.tom = convertToXMLcal(LocalDate.now().plusDays(5))

        var trygdetidListe = V1TrygdetidListe()
        trygdetidListe.trygdetidListe.add(ttid1)
        trygdetidListe.trygdetidListe.add(ttid2)
        trygdetidListe.trygdetidListe.add(ttid3)

        val result = dataFromPESYS.summerTrygdeTid(trygdetidListe)
        println("resultat: $result")
        assertEquals(15, result)

        val pendata = Pensjonsinformasjon()
        pendata.trygdetidListe = trygdetidListe

        val bolresult = dataFromPESYS.erTrygdeTid(pendata)
        //bod for lite i utland mindre en 30 dager?
        assertEquals(false, bolresult)

    }

    @Test
    fun `summerTrygdeTid med 500 dager totalt`() {
        var ttid1 = V1Trygdetid()
        ttid1.fom = convertToXMLcal(LocalDate.now().minusDays(700))
        ttid1.tom = convertToXMLcal(LocalDate.now().minusDays(200))

        var trygdetidListe = V1TrygdetidListe()
        trygdetidListe.trygdetidListe.add(ttid1)

        val result = dataFromPESYS.summerTrygdeTid(trygdetidListe)
        println("resultat: $result")
        assertEquals(500, result)

        val pendata = Pensjonsinformasjon()
        pendata.trygdetidListe = trygdetidListe

        val bolresult = dataFromPESYS.erTrygdeTid(pendata)
        //bod mye i utland mer en 360d.
        assertEquals(false, bolresult)
    }

    @Test
    fun `summerTrygdeTid til 0`() {
        var fom = LocalDate.now().minusDays(0)
        var tom = LocalDate.now().plusDays(0)
        var trygdetidListe = V1TrygdetidListe()
        var ttid1 = V1Trygdetid()
        ttid1.fom = convertToXMLcal(fom)
        ttid1.tom = convertToXMLcal(tom)
        trygdetidListe.trygdetidListe.add(ttid1)
        val result = dataFromPESYS.summerTrygdeTid(trygdetidListe)
        assertEquals(0, result)
    }

    fun convertToXMLcal(time: LocalDate): XMLGregorianCalendar {
        val gcal = GregorianCalendar()
        gcal.setTime(Date.from( time.atStartOfDay( ZoneId.systemDefault() ).toInstant() ))
        val xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
        return xgcal
    }


    @Test
    fun `create createAvlsagsBegrunnelse test for AlderPensjon og GjenlevP TrygdleListeTom`() {

        val pendata = generateFakePensjoninformasjonForALDER()
        pendata.vedtak.isBoddArbeidetUtland = true
        pendata.trygdetidListe.trygdetidListe.clear()
        val result = dataFromPESYS.createAvlsagsBegrunnelse(pendata)
        assertEquals("01", result)

        val pendata1 = generateFakePensjoninformasjonForGJENLEV()
        pendata1.vedtak.isBoddArbeidetUtland = true
        pendata1.trygdetidListe.trygdetidListe.clear()
        val result1 = dataFromPESYS.createAvlsagsBegrunnelse(pendata1)
        assertEquals("01", result1)
    }

    @Test
    fun `create createAvlsagsBegrunnelse test for AlderPensjon TrygdleListeTom`() {

        val pendata = generateFakePensjoninformasjonForUFOREP()
        pendata.vedtak.isBoddArbeidetUtland = true
        pendata.trygdetidListe.trygdetidListe.clear()
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).vilkarsvurderingUforetrygd.hensiktsmessigBehandling = "HENSIKTSMESSIG_BEH"
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).vilkarsvurderingUforetrygd.alder = ""
        val result = dataFromPESYS.createAvlsagsBegrunnelse(pendata)

        assertEquals("08", result)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun `sjekke for om pendata har sann verdi harBoddArbeidetUtland`() {
        prefill = generatePrefillData(66, "P6000")
        val pensjonsinformasjonService1 = PensjonsinformasjonService(pensjonsinformasjonRestTemplate, RequestBuilder())
        val dataFromPESYS2 = PrefillP6000PensionDataFromPESYS(pensjonsinformasjonService1)
        whenever(pensjonsinformasjonRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))).thenReturn(readXMLresponse("P6000-AP-101.xml"))
        dataFromPESYS2.prefill(prefill)
    }



    private fun generateFakePensjoninformasjonForALDER(): Pensjonsinformasjon {
        return generateFakePensjoninformasjonForKSAK("ALDER")
    }

    private fun generateFakePensjoninformasjonForUFOREP(): Pensjonsinformasjon {
        return generateFakePensjoninformasjonForKSAK("UFOREP")
    }

    private fun generateFakePensjoninformasjonForGJENLEV(): Pensjonsinformasjon {
        return generateFakePensjoninformasjonForKSAK("GJENLEV")
    }

    private fun generatePrefillData(subtractYear: Int, sedId: String): PrefillDataModel {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefill.apply {
            rinaSubject= "Pensjon"
            sed = createSED(sedId)
            penSaksnummer = "12345"
            vedtakId = "12312312"
            buc = "P_BUC_99"
            aktoerID = "123456789"
            personNr = generateRandomFnr(subtractYear)
            institution = items
        }
        return prefill
    }

    private fun generateRandomFnr(yearsToSubtract: Int) : String {
        val fnrdate = LocalDate.now().minusYears(yearsToSubtract.toLong())
        val y = fnrdate.year.toString()
        val day = fixDigits(fnrdate.dayOfMonth.toString())
        val month = fixDigits(fnrdate.month.value.toString())
        val fixedyear = y.substring(2,y.length)
        val fnr = day+month+fixedyear+43352
        return fnr
    }

    private fun fixDigits(str: String): String {
        if (str.length == 1) {
            return "0$str"
        }
        return str
    }

    private fun generateFakePensjoninformasjonForKSAK(ksak: String): Pensjonsinformasjon {
        val xmlcal = DatatypeFactory.newInstance().newXMLGregorianCalendar()

        val v1sak = V1Sak()
        v1sak.sakType = ksak // "ALDER"
        v1sak.isUttakFor67 = false
        val v1vedtak = V1Vedtak()

        v1vedtak.datoFattetVedtak = xmlcal
        v1vedtak.virkningstidspunkt = xmlcal

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

        val trygdetidListe = V1TrygdetidListe()

        val peninfo = Pensjonsinformasjon()
        peninfo.sak = v1sak
        peninfo.vedtak = v1vedtak

        peninfo.ytelsePerMaanedListe = ytelsePerMaanedListe
        peninfo.vilkarsvurderingListe = vilkarsvurderingListe
        peninfo.trygdetidListe = trygdetidListe

        return peninfo
    }

}