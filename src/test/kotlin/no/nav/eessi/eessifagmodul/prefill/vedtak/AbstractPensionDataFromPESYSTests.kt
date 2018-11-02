package no.nav.eessi.eessifagmodul.prefill.vedtak

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.Pensjon
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
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
import org.mockito.ArgumentMatchers
import org.mockito.Mock
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

abstract class AbstractPensionDataFromPESYSTests(private val xmlFilename: String) {

    @Mock
    lateinit var pensjonsinformasjonService: PensjonsinformasjonService

    @Mock
    lateinit var pensjonsinformasjonRestTemplate: RestTemplate

    protected lateinit var  prefill: PrefillDataModel
    protected lateinit var dataFromPESYS: PensionDataFromPESYS
    protected lateinit var dataFromPESYS1: PensionDataFromPESYS
    protected lateinit var pendata: Pensjonsinformasjon


    @Before
    fun setup() {
        prefill = PrefillDataModel()
        dataFromPESYS = PensionDataFromPESYS(pensjonsinformasjonService)
        dataFromPESYS1 = readPensionDataFromPESYS()
        pendata = readPensjonsinformasjon( dataFromPESYS1 )

    }

    fun readPensionDataFromPESYS(): PensionDataFromPESYS {
        return mockPrefillP6000PensionDataFromPESYS(xmlFilename)
    }

    fun readPensjonsinformasjon(penDatafromPesys: PensionDataFromPESYS) : Pensjonsinformasjon {
        return penDatafromPesys.getPensjoninformasjonFraVedtak("1234567")
    }

    fun readXMLresponse(file: String): ResponseEntity<String> {
        val resource = ResourceUtils.getFile("classpath:pensjonsinformasjon/$file").readText()
        return ResponseEntity(resource, HttpStatus.OK)
    }

    fun mockPrefillP6000PensionDataFromPESYS(responseXMLfilename: String): PensionDataFromPESYS {
        val pensjonsinformasjonService1 = PensjonsinformasjonService(pensjonsinformasjonRestTemplate, RequestBuilder())
        whenever(pensjonsinformasjonRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))).thenReturn(readXMLresponse(responseXMLfilename))
        return PensionDataFromPESYS(pensjonsinformasjonService1)
    }

    fun debugPrintFinalResult(result: Pensjon) {
        val sed = prefill.sed
        sed.pensjon = result
        val json = sed.toJson()
        println("vedtak")
        println("----------------------------------------------------------------------\n")
        println(json)
        println("----------------------------------------------------------------------\n")
    }


    fun generateFakePensjoninformasjonForALDER(): Pensjonsinformasjon {
        return generateFakePensjoninformasjonForKSAK("ALDER")
    }

    fun generateFakePensjoninformasjonForUFOREP(): Pensjonsinformasjon {
        return generateFakePensjoninformasjonForKSAK("UFOREP")
    }

    fun generateFakePensjoninformasjonForGJENLEV(): Pensjonsinformasjon {
        return generateFakePensjoninformasjonForKSAK("GJENLEV")
    }

    fun generatePrefillData(subtractYear: Int, sedId: String): PrefillDataModel {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefill.apply {
            rinaSubject= "Pensjon"
            sed = SED.create(sedId)
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
        //v1sak.isUttakFor67 = false
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
        peninfo.sak = v1sak
        peninfo.vedtak = v1vedtak

        peninfo.ytelsePerMaanedListe = ytelsePerMaanedListe
        peninfo.vilkarsvurderingListe = vilkarsvurderingListe
        peninfo.trygdetidListe = trygdetidListe

        return peninfo
    }

    private fun createTrygdelisteTid(): V1TrygdetidListe {
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

    private fun convertToXMLcal(time: LocalDate): XMLGregorianCalendar {
        val gcal = GregorianCalendar()
        gcal.setTime(Date.from( time.atStartOfDay( ZoneId.systemDefault() ).toInstant() ))
        val xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
        return xgcal
    }


    @Test(expected = IllegalArgumentException::class)
    fun `preutfylling P6000 feiler ved mangler av vedtakId`() {
        prefill = generatePrefillData(68, "vedtak")
        prefill.vedtakId = ""
        dataFromPESYS.prefill(prefill)

    }

    @Test
    fun `summerTrygdeTid forventet 10 dager, erTrygdeTid forventet til false`() {
        var ttid1 = V1Trygdetid()
        ttid1.fom = convertToXMLcal(LocalDate.now().minusDays(50))
        ttid1.tom = convertToXMLcal(LocalDate.now().minusDays(40))


        var trygdetidListe = V1TrygdetidListe()
        trygdetidListe.trygdetidListe.add(ttid1)

        val result = dataFromPESYS.summerTrygdeTid(trygdetidListe)
        println("resultat: $result")
        assertEquals(10, result)

        val pendata = Pensjonsinformasjon()
        pendata.trygdetidListe = trygdetidListe
        val bolresult = dataFromPESYS.erTrygdeTid(pendata)
        //bod i utland mindre totalt 10dager en mer en mindre en 30 og mindre en 360
        assertEquals(false, bolresult)

    }

    @Test
    fun `summerTrygdeTid forventet 70 dager, erTrygdeTid forventet til true`() {
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
    fun `summerTrygdeTid forventet 15 dager, erTrygdeTid forventet til false`() {
        val trygdetidListe = createTrygdelisteTid()

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
    fun `summerTrygdeTid forventet 500 dager, erTrygdeTid forventet til false`() {
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
    fun `summerTrygdeTid forventet 0`() {
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

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun `feiler ved boddArbeidetUtland ikke sann`() {
        prefill = generatePrefillData(66, "vedtak")
        val pensjonsinformasjonService1 = PensjonsinformasjonService(pensjonsinformasjonRestTemplate, RequestBuilder())
        val dataFromPESYS2 = PensionDataFromPESYS(pensjonsinformasjonService1)
        whenever(pensjonsinformasjonRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))).thenReturn(readXMLresponse("P6000-AP-101.xml"))
        dataFromPESYS2.prefill(prefill)
    }

    @Test
    fun `forventer "07" på AvlsagsBegrunnelse IKKE_MOTTATT_DOK`() {

        val pendata = generateFakePensjoninformasjonForALDER()
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "IKKE_MOTTATT_DOK"
        val result = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata)
        assertEquals("07", result)
    }


}