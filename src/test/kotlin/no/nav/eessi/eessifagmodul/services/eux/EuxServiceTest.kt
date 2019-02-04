package no.nav.eessi.eessifagmodul.services.eux

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.validateJson
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.String


@RunWith(MockitoJUnitRunner::class)
class EuxServiceTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EuxServiceTest::class.java) }

    private lateinit var service: EuxService

    @Mock
    private lateinit var mockrestTemplate: RestTemplate


    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        service = EuxService(mockrestTemplate)
    }

    @Test
    fun opprettUriComponentPath() {
        val path = "/buc/{RinaSakId}/sed"
        val uriParams = mapOf("RinaSakId" to "12345")
        val builder = UriComponentsBuilder.fromUriString(path)
                .queryParam("KorrelasjonsId", "c0b0c068-4f79-48fe-a640-b9a23bf7c920")
                .buildAndExpand(uriParams)
        val str = builder.toUriString()
        assertEquals("/buc/12345/sed?KorrelasjonsId=c0b0c068-4f79-48fe-a640-b9a23bf7c920", str)
    }


    //opprett buc og sed ok
    @Test
    fun `forventer korrekt svar tilbake fra et kall til opprettBucSed`() {
        val bucresp = BucSedResponse("123456", "2a427c10325c4b5eaf3c27ba5e8f1877")
        val response: ResponseEntity<String> = ResponseEntity(mapAnyToJson(bucresp), HttpStatus.OK)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val httpEntity = HttpEntity(SED("P2000").toJson(), headers)

        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenReturn(response)
        val result = service.opprettBucSed(SED("P2000"), "P_BUC_99", "NAVT003", "1234567")

        assertEquals("123456", result.caseId)
        assertEquals("2a427c10325c4b5eaf3c27ba5e8f1877", result.documentId)

    }

    //opprett buc og sed feiler ved oppreting
    @Test(expected = RinaCasenrIkkeMottattException::class)
    fun `feiler med svar tilbake fra et kall til opprettBucSed`() {
        val errorresponse = ResponseEntity<String?>(HttpStatus.BAD_REQUEST)
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenReturn(errorresponse)
        service.opprettBucSed(SED("P2200"), "P_BUC_99", "NAVT003", "1231233")
    }

    //opprett buc og sed feil med eux service
    @Test(expected = EuxServerException::class)
    fun `feiler med kontakt fra eux med kall til opprettBucSed`() {
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenThrow(RuntimeException::class.java)
        service.opprettBucSed(SED("P2000"), "P_BUC_99", "NAVT003", "213123")
    }


    //Test Hent Buc
    @Test
    fun `forventer korrekt svar tilbake fra et kall til hentbuc`() {
        val filepath = "src/test/resources/json/buc/buc-22909_v4.1.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        kotlin.test.assertTrue(validateJson(json))
        val response: ResponseEntity<String> = ResponseEntity(json, HttpStatus.OK)

        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(null), eq(String::class.java))).thenReturn(response)
        val result = service.getBuc("P_BUC_99")
        assertEquals("22909", result.id)
    }

    @Test(expected = BucIkkeMottattException::class)
    fun `feiler med svar tilbake fra et kall til hentbuc`() {
        val errorresponse = ResponseEntity<String?>("", HttpStatus.BAD_REQUEST)
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(null), eq(String::class.java))).thenReturn(errorresponse)
        service.getBuc("P_BUC_99")
    }

    @Test(expected = EuxServerException::class)
    fun `feiler med kontakt fra eux med kall til hentbuc`() {
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(null), eq(String::class.java))).thenThrow(RuntimeException::class.java)
        service.getBuc("P_BUC_99")
    }


    //opprett sed på en valgt buc ok
    @Test
    fun `forventer korrekt svar tilbake fra et kall til opprettSedOnBuc`() {
        val response: ResponseEntity<String> = ResponseEntity("323413415dfvsdfgq343145sdfsdfg34135", HttpStatus.OK)
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenReturn(response)

        val result = service.opprettSedOnBuc(SED("P2000"), "123456")

        assertEquals("123456", result.caseId)
        assertEquals("323413415dfvsdfgq343145sdfsdfg34135", result.documentId)
    }

    //opprett sed på en valgt buc, feiler ved oppreting
    @Test(expected = SedDokumentIkkeOpprettetException::class)
    fun `feiler med svar tilbake fra et kall til opprettSedOnBuc`() {
        val errorresponse = ResponseEntity<String?>(HttpStatus.BAD_REQUEST)
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenReturn(errorresponse)
        service.opprettSedOnBuc(SED("P2200"), "1231233")
    }

    //opprett sed på en valgt buc, feil med eux service
    @Test(expected = EuxServerException::class)
    fun `feiler med kontakt fra eux med kall til opprettSedOnBuc`() {
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenThrow(RuntimeException::class.java)
        service.opprettSedOnBuc(SED("P2000"), "213123")
    }


    @Test
    fun `forventer OK ved sletting av valgt SED på valgt buc`() {
        val response: ResponseEntity<String> = ResponseEntity(HttpStatus.OK)
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), eq(null), eq(String::class.java))).thenReturn(response)
        val result = service.deleteDocumentById("12132131", "12312312-123123123123")
        assertEquals(true, result)
    }


    @Test(expected = SedIkkeSlettetException::class)
    fun `feiler med svar tilbake fra et kall til deleteDocumentById`() {
        val response: ResponseEntity<String> = ResponseEntity(HttpStatus.NOT_EXTENDED)
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), eq(null), eq(String::class.java))).thenReturn(response)
        service.deleteDocumentById("12132131", "12312312-123123123123")
    }

    @Test(expected = EuxServerException::class)
    fun `feiler med kontakt fra eux med kall til deleteDocumentById`() {
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), eq(null), eq(String::class.java))).thenThrow(RuntimeException::class.java)
        service.deleteDocumentById("12132131", "12312312-123123123123")
    }

    @Test
    fun `forventer korrekt svar tilbake når SED er sendt OK på sendDocumentById`() {
        val response: ResponseEntity<String> = ResponseEntity(HttpStatus.OK)

        //val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, null, String::class.java).statusCode
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(null), eq(String::class.java))).thenReturn(response)
        val result = service.sendDocumentById("123456", "213213-123123-123123")
        assertEquals(true, result)
    }

    @Test(expected = SedDokumentIkkeSendtException::class)
    fun `feiler med svar tilbake fra et kall til sendDocumentById`() {
        val errorresponse = ResponseEntity<String?>(HttpStatus.BAD_REQUEST)
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(null), eq(String::class.java))).thenReturn(errorresponse)
        service.sendDocumentById("123456", "213213-123123-123123")
    }

    //opprett sed på en valgt buc, feil med eux service
    @Test(expected = EuxServerException::class)
    fun `feiler med kontakt fra eux med kall til sendDocumentById`() {
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(null), eq(String::class.java))).thenThrow(RuntimeException::class.java)
        service.sendDocumentById("123456", "213213-123123-123123")
    }


    // sendDocumentById


    //gamle tester som muligens utgår mot eux-app nye api


//    @Test
//    fun `check for mulige aksjoner on rinacaseid`() {
//        val bucType = "FB_BUC_01"
//
//        val data = "[" +
//                "{\"navn\":\"Create\"," +
//                "\"id\":\"312430_f54d4c4ea29840a3bd8404ec08ffd29f\",\n" +
//                "\"kategori\":\"Documents\",\n" +
//                "\"dokumentType\":\"P2000\"," +
//                "\"dokumentId\":\"602982a0a84d4fe6aaf46a61b30a3a2e\"}]"
//        val response: ResponseEntity<String> = ResponseEntity(data, HttpStatus.OK)
//        whenever(mockrestTemplate.exchange(anyString(),eq(HttpMethod.GET), any(), eq(typeRef<String>()))).thenReturn(response)
//
//        val resultat = service.getPossibleActions(bucType)
//
//        assertNotNull(resultat)
//
//        val aksjon = resultat[0]
//
//        assertNotNull(aksjon)
//        assertEquals("P2000", aksjon.dokumentType)
//        assertEquals("Create", aksjon.navn)
//    }

//
//    @Test
//    fun createCaseAndDocument() {
//
//        val fagSaknr = "EESSI-PEN-123"
//        val mottaker = "DUMMY"
//
//        val sed = SedMock().genererP6000Mock()
//        //val sedAsJson = mapAnyToJson(sed)
//
//        val bucType = "P_BUC_06" //p6000
//        //val bucType = "P_BUC_01" //p2000
//
//        val korrid = UUID.randomUUID()
//        val vedleggType = ""
//
//        //mock response and mock restTemplate
//        val response: ResponseEntity<String> = ResponseEntity("12345", HttpStatus.OK)
//        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenReturn(response)
//
//        try {
//            val data = service.createCaseAndDocument(sed, bucType, fagSaknr, mottaker, vedleggType, korrid.toString())
//            assertTrue("Skal komme hit!", true)
//            logger.info("Response: $data")
//            assertEquals("12345", data)
//            assertNotNull(data)
//            println("RINA NR?       : $data")
//            println("KorrelasjonsID : $korrid")
//            println("BuCType        : $bucType")
//        } catch (ex: Exception) {
//            logger.error(ex.message)
//            fail("Skal ikke komme hit")
//        }
//    }

}

