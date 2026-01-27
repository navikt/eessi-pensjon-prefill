package no.nav.eessi.pensjon.services.pensjonsinformasjon

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.ResourceUtils
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate

@ActiveProfiles("test")
class PensjonsinformasjonClientTest {

    private var mockrestTemplate: RestTemplate = mockk()

//    private lateinit var pensjonsinformasjonClient: PensjonsinformasjonClient

    @BeforeEach
    fun setup() {
//        pensjonsinformasjonClient = PensjonsinformasjonClient(mockrestTemplate, PensjonRequestBuilder())
    }

    @Test
    fun hentAlt() {
        val mockResponseEntity = createResponseEntityFromJsonFile("classpath:pensjonsinformasjon/full-generated-response.xml")
        every { mockrestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns mockResponseEntity
//        val data = pensjonsinformasjonClient.hentAltPaaVedtak("1243")

//        assertNotNull(data.vedtak, "Vedtak er null")
//        assertEquals("2016-09-11", data.vedtak.virkningstidspunkt.simpleFormat())
    }

    @Test
    fun `PensjonsinformasjonClient  hentAlt paa vedtak feiler`() {
        every { mockrestTemplate.exchange(
            any<String>(),
            any(),
            any<HttpEntity<Unit>>(),
            eq(String::class.java)) } throws ResourceAccessException("IOException")

//        assertThrows<PensjoninformasjonException> {
//            pensjonsinformasjonClient.hentAltPaaVedtak("1243")
        }
//    }


//    @Test
//    fun `Sjekker om pensjoninformasjon XmlCalendar kan være satt eller null sette simpleFormat`() {
//        val mockResponseEntity = createResponseEntityFromJsonFile("classpath:pensjonsinformasjon/full-generated-response.xml")
//
//        every { mockrestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns mockResponseEntity
//        val data = pensjonsinformasjonClient.hentAltPaaVedtak("1243")
//
//        var result = data.ytelsePerMaanedListe.ytelsePerMaanedListe.first()
//
//        assertEquals("2008-02-06", result.fom.simpleFormat())
//        assertEquals("2015-08-04", result.tom?.simpleFormat())
//
//        result = data.ytelsePerMaanedListe.ytelsePerMaanedListe.getOrNull(1)
//
//        assertNotNull(result)
//        assertNotNull(result.fom)
//
//        assertEquals("2008-02-06", result.fom.simpleFormat())
//        assertEquals(null, result.tom?.simpleFormat())
//
//    }

//    @Test
//    fun `hentAltpaaSak  mock data med to saktyper en skal komme ut`() {
//        val mockResponseEntity = createResponseEntityFromJsonFile("classpath:pensjonsinformasjon/krav/KravAlderEllerUfore_AP_UTLAND.xml")
//        every { mockrestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java))} returns  (mockResponseEntity)
//
//        val data = pensjonsinformasjonClient.hentAltPaaFNR("4234234")
//        val sak = FinnSak.finnSak("21975717", data)!!
//
//        assertEquals("21975717", sak.sakId.toString())
//        assertEquals("ALDER", sak.sakType)
//    }

//    @Test
//    fun `hentAltpaaSak  mock data med aktoerid to saktyper en skal komme ut`() {
//        val mockResponseEntity = createResponseEntityFromJsonFile("classpath:pensjonsinformasjon/krav/KravAlderEllerUfore_AP_UTLAND.xml")
//        every { mockrestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns (mockResponseEntity)
//
//        @Suppress("DEPRECATION")
//        val data = pensjonsinformasjonClient.hentAltPaaAktoerId("123456789011")
//
//        assertEquals(2, data.brukersSakerListe.brukersSakerListe.size)
//
//        val sak = FinnSak.finnSak("21975717", data)
//
//        sak?.let {
//            assertEquals("21975717", it.sakId.toString())
//            assertEquals("ALDER", it.sakType)
//        }
//    }

//    @Test
//    fun `hentAltpåSak  mock data med tom aktoerid to saktyper en skal komme ut`() {
//        val strAktor = ""
//        assertThrows<IllegalArgumentException> {
//            @Suppress("DEPRECATION")
//            pensjonsinformasjonClient.hentAltPaaAktoerId(strAktor)
//        }
//    }




//    @Test
//    fun `hentPensjonSakType   mock response ok`() {
//        val mockResponseEntity = createResponseEntityFromJsonFile("classpath:pensjonsinformasjon/krav/AP_FORSTEG_BH.xml")
//
//        every { mockrestTemplate.exchange(any<String>(), any(), any(), eq(String::class.java)) } returns mockResponseEntity
//
//        val response = pensjonsinformasjonClient.hentKunSakType("22580170", "12345678901")
//
//        assertEquals("ALDER", response.sakType)
//        assertEquals("22580170", response.sakId)
//
//    }

//    @Test
//    fun `hentPensjonSakType   mock response ingen sak eller data`() {
//        val mockResponseEntity = createResponseEntityFromJsonFile("classpath:pensjonsinformasjon/empty-pensjon-response.xml")
//        val sakid = "22580170"
//
//        every { mockrestTemplate.exchange(any<String>(), any(), any(), eq(String::class.java)) } returns mockResponseEntity
//
//        val res =  pensjonsinformasjonClient.hentKunSakType(sakid,  "12345678901")
//        assertEquals("", res.sakType)
//        assertEquals(sakid, res.sakId)
//    }

//    @Test
//    fun `transform en gyldig xmlString til Persjoninformasjon forventer et gyldig object`() {
//        val listOfxml = listOf("vedtak/P6000-APUtland-301.xml","krav/AP_FORSTEG_BH.xml","vedtak/P6000-UF-Avslag.xml","empty-pensjon-response.xml")
//
//        //kjøre igjennom alle tester så ser vi!
//        listOfxml.forEach { xmlFile ->
//            val xml = ResourceUtils.getFile("classpath:pensjonsinformasjon/$xmlFile").readText()
//            val actual = pensjonsinformasjonClient.transform(xml)
//
//            assertNotNull(actual)
//            assertEquals(Pensjonsinformasjon::class.java, actual::class.java)
//        }
//    }

//    @Test
//    fun `transform en IKKE gyldig xmlString til Persjoninformasjon forventer excpetion`() {
//        val xml = "fqrqadfgadf gad23423fsdvdf"
//        assertThrows<PensjoninformasjonProcessingException> {
//            pensjonsinformasjonClient.transform(xml)
//        }
//    }

//    @Test
//    fun `transform en tom xmlString til Persjoninformasjon forventer excpetion`() {
//        assertThrows<PensjoninformasjonProcessingException> {
//            pensjonsinformasjonClient.transform("")
//        }
//    }

//    @Test
//    fun `transform en json til Persjoninformasjon forventer excpetion`() {
//        val json = "\"land\": {\n" +
//                "      \"value\": \"23123\",\n" +
//                "      \"kodeRef\": null,\n" +
//                "      \"kodeverksRef\": \"kodeverk\"\n" +
//                "}\n"
//        assertThrows<PensjoninformasjonProcessingException> {
//            pensjonsinformasjonClient.transform(json)
//        }
//    }

//    @Test
//    fun `hentKravDato henter dato fra brukersSakerListe `() {
//        val kravId = "41098601"
//        val saksId = "14915730"
//
//        mockAnyRequest("classpath:pensjonsinformasjon/krav/KravAlderEllerUfore_AP_UTLAND.xml")
//        val actual = pensjonsinformasjonClient.hentKravDatoFraAktor("any", saksId, kravId)
//        assertEquals("2018-05-04", actual)
//    }

    private fun mockAnyRequest(kravLokasjon : String) {
        val mockResponseEntity = createResponseEntityFromJsonFile(kravLokasjon)
//        every { mockrestTemplate.exchange(any<String>(), any(), any(), eq(String::class.java)) } returns mockResponseEntity
    }

    private fun createResponseEntityFromJsonFile(filePath: String, httpStatus: HttpStatus = HttpStatus.OK): ResponseEntity<String> {
        val mockResponseString = ResourceUtils.getFile(filePath).readText()
        return ResponseEntity(mockResponseString, httpStatus)
    }

}
