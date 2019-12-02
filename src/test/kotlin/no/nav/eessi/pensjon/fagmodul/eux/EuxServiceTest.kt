package no.nav.eessi.pensjon.fagmodul.eux

import com.nhaarman.mockitokotlin2.*
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.BucSedResponse
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.Rinasak
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.PinItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.logging.RequestResponseLoggerInterceptor
import no.nav.eessi.pensjon.utils.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.*
import org.springframework.web.util.UriComponentsBuilder
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import kotlin.String


@ExtendWith(MockitoExtension::class)
class EuxServiceTest {

    private lateinit var service: EuxService

    @Mock
    private lateinit var mockEuxrestTemplate: RestTemplate


    @BeforeEach
    fun setup() {
        mockEuxrestTemplate.errorHandler = DefaultResponseErrorHandler()
        mockEuxrestTemplate.interceptors = listOf( RequestResponseLoggerInterceptor() )
        service = EuxService(mockEuxrestTemplate)
    }

    @AfterEach
    fun takedown() {
        Mockito.reset(mockEuxrestTemplate)
    }

    @Test
    fun `Opprett Uri component path`() {
        val path = "/type/{RinaSakId}/sed"
        val uriParams = mapOf("RinaSakId" to "12345")
        val builder = UriComponentsBuilder.fromUriString(path)
                .queryParam("KorrelasjonsId", "c0b0c068-4f79-48fe-a640-b9a23bf7c920")
                .buildAndExpand(uriParams)
        val str = builder.toUriString()
        assertEquals("/type/12345/sed?KorrelasjonsId=c0b0c068-4f79-48fe-a640-b9a23bf7c920", str)
    }


    //opprett type og sed ok
    @Test
    fun `Calling EuxService  forventer korrekt svar tilbake fra et kall til opprettBucSed`() {
        val bucresp = BucSedResponse("123456", "2a427c10325c4b5eaf3c27ba5e8f1877")
        val response: ResponseEntity<String> = ResponseEntity(mapAnyToJson(bucresp), HttpStatus.OK)

        whenever(mockEuxrestTemplate.exchange(any<String>(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenReturn(response)
        val result = service.opprettBucSed(SED("P2000"), "P_BUC_99", "NO:NAVT003", "1234567")

        assertEquals("123456", result.caseId)
        assertEquals("2a427c10325c4b5eaf3c27ba5e8f1877", result.documentId)

    }

    //opprett type og sed feiler ved oppreting
    @Test
    fun `Calling EuxService  feiler med svar tilbake fra et kall til opprettBucSed`() {
        val errorresponse = ResponseEntity<String?>(HttpStatus.BAD_REQUEST)
        whenever(mockEuxrestTemplate.exchange(any<String>(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenReturn(errorresponse)

        assertThrows<EuxRinaServerException> {
            service.opprettBucSed(SED("P2200"), "P_BUC_99", "NO:NAVT003", "1231233")
        }
    }

    //opprett type og sed feil med eux service
    @Test
    fun `Calling EuxService  feiler med kontakt fra eux med kall til opprettBucSed`() {
        whenever(mockEuxrestTemplate.exchange(any<String>(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenThrow(RuntimeException::class.java)
        assertThrows<EuxServerException> {
            service.opprettBucSed(SED("P2000"), "P_BUC_99", "NO:NAVT003", "213123")
        }
    }

    @Test
    fun `forventer et korrekt navsed P6000 ved kall til getSedOnBucByDocumentId`() {
        val filepath = "src/test/resources/json/nav/P6000-NAV.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))

        val orgsed = SED.fromJson(json)
        val response: ResponseEntity<String> = ResponseEntity(json, HttpStatus.OK)

        //val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, null, String::class.java)
        whenever(mockEuxrestTemplate.exchange(any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))).thenReturn(response)

        val result = service.getSedOnBucByDocumentId("12345678900", "0bb1ad15987741f1bbf45eba4f955e80")

        assertEquals(orgsed, result)
        assertEquals("P6000", result.sed)

    }

    @Test
    fun `Calling EuxService  feiler med kontakt fra eux med kall til getSedOnBucByDocumentId`() {
        doThrow(createDummyServerRestExecption(HttpStatus.BAD_GATEWAY, "Dummybody"))
                .whenever(mockEuxrestTemplate).exchange(any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        assertThrows<GenericUnprocessableEntity> {
            service.getSedOnBucByDocumentId("12345678900", "P_BUC_99")
        }
    }

    @Test
    fun `Calling EuxService  feiler med motta navsed fra eux med kall til getSedOnBucByDocumentId`() {
        val errorresponse = ResponseEntity<String?>(HttpStatus.UNAUTHORIZED)
        whenever(mockEuxrestTemplate.exchange(any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))).thenReturn(errorresponse)
        assertThrows<SedDokumentIkkeLestException> {
            service.getSedOnBucByDocumentId("12345678900", "P_BUC_99")
        }
    }

    //Test Hent Buc
    @Test
    fun `Calling EuxService  forventer korrekt svar tilbake fra et kall til hentbuc`() {
        val filepath = "src/test/resources/json/buc/buc-22909_v4.1.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))
        val response: ResponseEntity<String> = ResponseEntity(json, HttpStatus.OK)

        whenever(mockEuxrestTemplate.exchange(any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))).thenReturn(response)
        val result = service.getBuc("P_BUC_99")
        assertEquals("22909", result.id)
    }

    fun createDummyServerRestExecption(httpstatus: HttpStatus, dummyBody: String)
            = HttpServerErrorException.create (httpstatus, httpstatus.name, HttpHeaders(), dummyBody.toByteArray(), Charset.defaultCharset())

    fun createDummyClientRestExecption(httpstatus: HttpStatus, dummyBody: String)
            = HttpClientErrorException.create (httpstatus, httpstatus.name, HttpHeaders(), dummyBody.toByteArray(), Charset.defaultCharset())

    @Test
    fun `Calling EuxService feiler med BAD_REQUEST fra kall til getBuc`() {
        doThrow(HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .whenever(mockEuxrestTemplate).exchange( any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))
        assertThrows<GenericUnprocessableEntity> {
            service.getBuc("P_BUC_99")
        }
    }

    @Test
    fun `Calling EuxService feiler med en UNAUTHORIZED Exception fra kall til hentbuc`() {
        doThrow(HttpClientErrorException(HttpStatus.UNAUTHORIZED))
                .whenever(mockEuxrestTemplate).exchange( any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))
        assertThrows<RinaIkkeAutorisertBrukerException> {
            service.getBuc("P_BUC_99")
        }
    }

    @Test
    fun `Calling EuxService feiler med en FORBIDDEN Exception fra kall til hentbuc`() {
        doThrow(createDummyClientRestExecption(HttpStatus.FORBIDDEN, "Gateway body dummy timeout"))
                .whenever(mockEuxrestTemplate).exchange( any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))
        assertThrows<ForbiddenException> {
            service.getBuc("P_BUC_99")
        }
    }

    @Test
    fun `Calling EuxService feiler med en NOT FOUND Exception fra kall til hentbuc`() {
        doThrow(createDummyClientRestExecption(HttpStatus.NOT_FOUND, "Gateway body dummy timeout"))
                .whenever(mockEuxrestTemplate).exchange( any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))
        assertThrows<IkkeFunnetException> {
            service.getBuc("P_BUC_99")
        }
    }

    @Test
    fun `Calling EuxService feiler med en UNPROCESSABLE ENTITY Exception fra kall til hentbuc`() {
        doThrow(createDummyClientRestExecption(HttpStatus.UNPROCESSABLE_ENTITY, "unprocesable dummy timeout"))
                .whenever(mockEuxrestTemplate).exchange( any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))
        assertThrows<GenericUnprocessableEntity> {
            service.getBuc("P_BUC_99")
        }
    }

    @Test
    fun `Calling EuxService kaster en GATEWAY_TIMEOUT Exception ved kall til hentbuc`() {
        doThrow(createDummyServerRestExecption(HttpStatus.GATEWAY_TIMEOUT, "Gateway body dummy timeout"))
                .whenever(mockEuxrestTemplate).exchange( any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))
        assertThrows<GatewayTimeoutException> {
            service.getBuc("P_BUC_99")
        }
    }

    @Test
    fun `Euxservice kaster en IO_EXCEPTION ved kall til getBuc`() {
        doThrow(RuntimeException(HttpStatus.I_AM_A_TEAPOT.name))
                .whenever(mockEuxrestTemplate).exchange( any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))
                assertThrows<ServerException> {
            service.getBuc("P_BUC_99")
        }
    }

    @Test
    fun `getBuc mock response HttpStatus NOT_FOUND excpecting IkkeFunnetException`() {
        doThrow(createDummyClientRestExecption(HttpStatus.NOT_FOUND,"Dummy body for Not Found exception"))
                .whenever(mockEuxrestTemplate).exchange( any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))
        assertThrows<IkkeFunnetException> {
            service.getBuc("P_BUC_99")
        }
    }

    @Test
    fun `EuxService  forventer korrekt svar tilbake fra et kall til opprettSedOnBuc`() {
        val response: ResponseEntity<String> = ResponseEntity("323413415dfvsdfgq343145sdfsdfg34135", HttpStatus.OK)
        whenever(mockEuxrestTemplate.exchange(any<String>(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenReturn(response)

        val result = service.opprettSedOnBuc(SED("P2000"), "123456")

        assertEquals("123456", result.caseId)
        assertEquals("323413415dfvsdfgq343145sdfsdfg34135", result.documentId)
    }

    //opprett sed på en valgt type, feiler ved oppreting
    @Test
    fun `Calling EuxService  feiler med svar tilbake fra et kall til opprettSedOnBuc`() {
        doThrow(createDummyClientRestExecption(HttpStatus.BAD_REQUEST, "Dummy clent error"))
                .whenever(mockEuxrestTemplate).exchange(
                        any<String>(),
                        eq(HttpMethod.POST),
                        any(),
                        eq(String::class.java)
                )

        assertThrows<GenericUnprocessableEntity> {
            service.opprettSedOnBuc(SED("P2200"), "1231233")
        }

    }

    @Test
    fun `Calling EuxService  feiler med kontakt fra eux med kall til opprettSedOnBuc forventer GatewayTimeoutException`() {
        doThrow(createDummyServerRestExecption(HttpStatus.GATEWAY_TIMEOUT,"Dummy body"))
                .whenever(mockEuxrestTemplate).exchange(
                        any<String>(),
                        eq(HttpMethod.POST),
                        any(),
                        eq(String::class.java)
                )
        assertThrows<GatewayTimeoutException> {
            service.opprettSedOnBuc(SED("P2000"), "213123")
        }
    }

    @Test
    fun testMapsParams() {
        val uriParams1 = mapOf("RinaSakId" to "121312", "DokuemntId" to null).filter { it.value != null }
        assertEquals(1, uriParams1.size)
        val uriParams2 = mapOf("RinaSakId" to "121312", "DokuemntId" to "98d6879827594d1db425dbdfef399ea8")
        assertEquals(2, uriParams2.size)
    }

    @Test
    fun `Calling EuxService  forventer OK ved sletting av valgt SED paa valgt buc`() {
        val response: ResponseEntity<String> = ResponseEntity(HttpStatus.OK)
        val euxCaseId = "123456"
        val documentId = "213213-123123-123123"

        doReturn(response).whenever(mockEuxrestTemplate).exchange(
                eq("/buc/${euxCaseId}/sed/${documentId}"),
                any(),
                eq(null),
                ArgumentMatchers.eq(String::class.java)
        )

        val result = service.deleteDocumentById(euxCaseId, documentId)
        assertEquals(true, result)
    }


    @Test
    fun `Calling EuxService  feiler med svar tilbake fra et kall til deleteDocumentById`() {
        val response: ResponseEntity<String> = ResponseEntity(HttpStatus.NOT_EXTENDED)
        whenever(mockEuxrestTemplate.exchange(any<String>(), eq(HttpMethod.DELETE), eq(null), eq(String::class.java))).thenReturn(response)
        assertThrows<SedIkkeSlettetException> {
            service.deleteDocumentById("12132131", "12312312-123123123123")
        }
    }

    @Test
    fun `Calling EuxService  feiler med kontakt fra eux med kall til deleteDocumentById`() {
        //whenever(mockEuxrestTemplate.exchange(any<String>(), eq(HttpMethod.DELETE), eq(null), eq(String::class.java))).thenThrow(RuntimeException::class.java)

        val euxCaseId = "123456"
        val documentId = "213213-123123-123123"

        doThrow(RuntimeException("error")).whenever(mockEuxrestTemplate).exchange(
                eq("/type/${euxCaseId}/sed/${documentId}/send"),
                any(),
                eq(null),
                ArgumentMatchers.eq(String::class.java)
        )

        assertThrows<EuxServerException> {
            service.deleteDocumentById(euxCaseId, documentId)
        }
    }

    @Test
    fun `Calling EuxService  forventer korrekt svar tilbake naar SED er sendt OK paa sendDocumentById`() {
        val response: ResponseEntity<String> = ResponseEntity(HttpStatus.OK)
        val euxCaseId = "123456"
        val documentId = "213213-123123-123123"

        doReturn(response).whenever(mockEuxrestTemplate).exchange(
                eq("/buc/${euxCaseId}/sed/${documentId}/send"),
                any(),
                eq(null),
                ArgumentMatchers.eq(String::class.java)
        )

        val result = service.sendDocumentById(euxCaseId, documentId)
        assertEquals(true, result)
    }

    @Test
    fun `Calling EuxService  feiler med svar tilbake fra et kall til sendDocumentById`() {
        val euxCaseId = "123456"
        val documentId = "213213-123123-123123"
        val errorresponse = ResponseEntity.badRequest().body("")

        doReturn(errorresponse).whenever(mockEuxrestTemplate).exchange(
                eq("/buc/${euxCaseId}/sed/${documentId}/send"),
                any(),
                eq(null),
                ArgumentMatchers.eq(String::class.java)
        )
        assertThrows<SedDokumentIkkeSendtException> {
            service.sendDocumentById(euxCaseId, documentId)
        }
    }

    //opprett sed på en valgt type, feil med eux service
    @Test
    fun `Calling EuxService  feiler med kontakt fra eux med kall til sendDocumentById`() {
        doThrow(RuntimeException("error")).whenever(mockEuxrestTemplate).exchange(
                eq("/type/1234567/sed/3123sfdf23-4324svfsdf324/send"),
                any(),
                eq(null),
                ArgumentMatchers.eq(String::class.java)
        )
        assertThrows<EuxServerException> {
            service.sendDocumentById("123456", "213213-123123-123123")
        }
    }


    @Test
    fun callingEuxServiceListOfRinasaker_Ok() {
        val filepathRinasaker = "src/test/resources/json/rinasaker/rinasaker_12345678901.json"
        val jsonRinasaker = String(Files.readAllBytes(Paths.get(filepathRinasaker)))
        assertTrue(validateJson(jsonRinasaker))
        val orgRinasaker = mapJsonToAny(jsonRinasaker, typeRefs<List<Rinasak>>())

        val responseMangeSaker: ResponseEntity<String> = ResponseEntity(jsonRinasaker, HttpStatus.OK)
        doReturn(responseMangeSaker).whenever(mockEuxrestTemplate).exchange(
                eq("/rinasaker?fødselsnummer=12345678900&rinasaksnummer=&buctype=&status="),
                eq(HttpMethod.GET),
                eq(null),
                eq(String::class.java))

        val filepathEnRinasak = "src/test/resources/json/rinasaker/rinasaker_ensak.json"
        val jsonEnRinasak = String(Files.readAllBytes(Paths.get(filepathEnRinasak)))
        assertTrue(validateJson(jsonEnRinasak))
        val responseEnsak: ResponseEntity<String> = ResponseEntity(jsonEnRinasak, HttpStatus.OK)


        doReturn(responseEnsak).whenever(mockEuxrestTemplate).exchange(
                eq("/rinasaker?fødselsnummer=&rinasaksnummer=123456&buctype=&status="),
                eq(HttpMethod.GET),
                eq(null),
                eq(String::class.java))

        val result = service.getRinasaker("12345678900", listOf("123456","83637"))

        assertEquals(154, orgRinasaker.size)
        assertEquals(orgRinasaker.size + 1, result.size)
    }

    @Test
    fun callingEuxServiceListOfRinasaker_IOError() {
        doThrow(createDummyServerRestExecption(HttpStatus.INTERNAL_SERVER_ERROR,"Serverfeil, I/O-feil"))
                .whenever(mockEuxrestTemplate).exchange(any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))
        assertThrows<EuxRinaServerException> {
            service.getRinasaker("12345678900", listOf("1", "2", "3"))
        }
    }

    @Test
    fun callingEuxServiceListOfRinasaker_ClientError() {
        doThrow(createDummyClientRestExecption(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED"))
                .whenever(mockEuxrestTemplate).exchange(any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))
                        assertThrows<RinaIkkeAutorisertBrukerException> {
            service.getRinasaker("12345678900", listOf("1", "2", "3"))
        }
    }

    @Test
    fun callingEuxServiceListOfRinasaker_ServerError() {

        doThrow(createDummyServerRestExecption(HttpStatus.BAD_GATEWAY, "Dummybody"))
                .whenever(mockEuxrestTemplate).exchange(any<String>(), eq(HttpMethod.GET), eq(null), eq(String::class.java))
        assertThrows<GenericUnprocessableEntity> {
            service.getRinasaker("12345678900", listOf("1", "2", "3"))
        }
    }

    @Test
    fun `Calling eux-rina-api to create BucSedAndView for Frontend all OK excpect valid json`() {
        val rinasakerjson = "src/test/resources/json/rinasaker/rinasaker_34567890111.json"
        val rinasakStr = String(Files.readAllBytes(Paths.get(rinasakerjson)))
        assertTrue(validateJson(rinasakStr))

        doReturn(ResponseEntity.ok(rinasakStr))
                .whenever(mockEuxrestTemplate).exchange(
                    eq("/rinasaker?fødselsnummer=12345678900&rinasaksnummer=&buctype=&status="),
                    eq(HttpMethod.GET), anyOrNull(), eq(String::class.java) )

        val rinasakresult = service.getRinaSakerFilterKunRinaId("12345678900", listOf("1"))

        val orgRinasaker = mapJsonToAny(rinasakStr, typeRefs<List<Rinasak>>())

        val bucjson = "src/test/resources/json/buc/buc-158123_2_v4.1.json"
        val bucStr = String(Files.readAllBytes(Paths.get(bucjson)))
        assertTrue(validateJson(bucStr))
        doReturn(ResponseEntity.ok(bucStr))
                .whenever(mockEuxrestTemplate)
                .exchange( ArgumentMatchers.contains("buc/") ,
                eq(HttpMethod.GET), eq(null), eq(String::class.java))

        //"001122334455"
        val result = service.getBucAndSedView(rinasakresult)

        assertNotNull(result)
        assertEquals(6, orgRinasaker.size)
        assertEquals(3, result.size)

        val firstJson = result.first()
        assertEquals("158123", firstJson.caseId)

        var lastUpdate: Long = 0
        firstJson.lastUpdate?.let { lastUpdate = it }
        assertEquals("2019-05-20T16:35:34",  Instant.ofEpochMilli(lastUpdate).atZone(ZoneId.systemDefault()).toLocalDateTime().toString())
        assertEquals(18, firstJson.seds?.size)

        val json = firstJson.toJson()
        println(json)

        val bucdetaljerpath = "src/test/resources/json/buc/bucdetaljer-158123.json"
        val bucdetaljer = String(Files.readAllBytes(Paths.get(bucdetaljerpath)))
        assertTrue(validateJson(bucdetaljer))
        JSONAssert.assertEquals(bucdetaljer, json, true)
    }

    @Test
    fun callingEuxServiceForSinglemenuUI_AllOK() {
        val bucjson = "src/test/resources/json/buc/buc-158123_2_v4.1.json"
        val bucStr = String(Files.readAllBytes(Paths.get(bucjson)))
        assertTrue(validateJson(bucStr))

        doReturn(ResponseEntity.ok(bucStr))
                .whenever(mockEuxrestTemplate)
                .exchange(
                        ArgumentMatchers.contains("/buc/158123") ,
                        eq(HttpMethod.GET),
                        eq(null),
                        eq(String::class.java)
                )

        val firstJson = service.getSingleBucAndSedView("158123")

        assertEquals("158123", firstJson.caseId)
        var lastUpdate: Long = 0
        firstJson.lastUpdate?.let { lastUpdate = it }
        assertEquals("2019-05-20T16:35:34",  Instant.ofEpochMilli(lastUpdate).atZone(ZoneId.systemDefault()).toLocalDateTime().toString())
        assertEquals(18, firstJson.seds?.size)
    }

    @Test
    fun callingEuxServiceCreateBuc_Ok() {

        val mockBuc = "12345678909999"
        val response: ResponseEntity<String> = ResponseEntity("12345678909999", HttpStatus.OK)

        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                eq(null),
                eq(String::class.java))
        ).thenReturn(response)

        val result = service.createBuc("P_BUC_01")

        println("response: $response")
        assertEquals(mockBuc, result)
    }

    @Test
    fun callingEuxServiceCreateBuc_IOError() {
        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                eq(null),
                eq(String::class.java))
        ).thenThrow(ResourceAccessException("I/O error"))

        assertThrows<ServerException> {
            service.createBuc("P_BUC_01")
        }
    }

    @Test
    fun callingEuxServiceCreateBuc_ClientError() {
        val clientError = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Error in Token", HttpHeaders(), "Error in Token".toByteArray(), Charset.defaultCharset())
        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                eq(null),
                eq(String::class.java))
        ).thenThrow(clientError)

        assertThrows<RinaIkkeAutorisertBrukerException> {
            service.createBuc("P_BUC_01")
        }

    }

    @Test
    fun callingEuxServiceCreateBuc_ServerError() {

        val serverError = HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Error in Gate", HttpHeaders(), "Error in Gate".toByteArray(), Charset.defaultCharset())
        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                eq(null),
                eq(String::class.java))
        ).thenThrow(serverError)

        assertThrows<EuxRinaServerException> {
            service.createBuc("P_BUC_01")
        }
    }

    @Test
    fun callingEuxServicePutBucDeltager_WrongParticipantInput() {
        assertThrows<IllegalArgumentException> {
            service.putBucMottakere("126552", listOf(InstitusjonItem("NO", "NAVT", "Dummy")))
        }
    }

    @Test
    fun `call putBucMottakere feiler med UNAUTHORIZED forventer RinaIkkeAutorisertBrukerException`() {
        val clientError = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Token authorization error", HttpHeaders(),"Token authorization error".toByteArray(),Charset.defaultCharset())
        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.PUT),
                eq(null),
                eq(String::class.java))
        ).thenThrow(clientError)

        assertThrows<RinaIkkeAutorisertBrukerException> {
            service.putBucMottakere("126552", listOf(InstitusjonItem("NO", "NO:NAVT007", "NAV")))
        }
    }

    @Test
    fun `call putBucMottaker feiler ved INTERNAL_SERVER_ERROR forventer UgyldigCaseIdException`() {
        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.PUT),
                eq(null),
                eq(String::class.java))
        ).thenThrow(createDummyServerRestExecption(HttpStatus.INTERNAL_SERVER_ERROR,"Dummy Internal Server Error body"))

        assertThrows<EuxRinaServerException> {
            service.putBucMottakere("122732", listOf(InstitusjonItem("NO", "NO:NAVT02", "NAV")))
        }
    }


    @Test
    fun putBucDeltager_ResourceAccessError() {
        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.PUT),
                eq(null),
                eq(String::class.java))
        ).thenThrow(ResourceAccessException("Other unknown Error"))

        assertThrows<ServerException> {
            service.putBucMottakere("122732", listOf(InstitusjonItem("NO", "NO:NAVT02", "NAV")))
        }
    }

    @Test
    fun putBucDeltager_RuntimeExceptionError() {
        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.PUT),
                eq(null),
                eq(String::class.java))
        ).thenThrow(RuntimeException("Error"))

        assertThrows<RuntimeException> {
            service.putBucMottakere("122732", listOf(InstitusjonItem("NO", "NO:NAVT02", "NAV")))
        }
    }

    @Test
    fun callingPutBucDeltager_OK() {

        val theResponse = ResponseEntity.ok().body("")

        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.PUT),
                eq(null),
                ArgumentMatchers.eq(String::class.java))
        ).thenReturn(theResponse)

        val result = service.putBucMottakere("122732", listOf(InstitusjonItem("NO","NO:NAVT005","NAV")))
        assertEquals(true, result)
    }

    @Test
    fun hentNorskFnrPaalisteavPin() {
        val list = listOf(
                PinItem(sektor = "03", land = "SE", identifikator = "00987654321", institusjonsnavn = "SE"),
                PinItem(sektor = "02", land = "NO", identifikator = "12345678900", institusjonsnavn = "NAV"),
                PinItem(sektor = "02", land = "DK", identifikator = "05467898321", institusjonsnavn = "DK")
            )

        val result = service.getFnrMedLandkodeNO(list)
        assertEquals("12345678900", result)
    }

    @Test
    fun hentNorskFnrPaalisteavPinListeTom() {
        val result = service.getFnrMedLandkodeNO(listOf())
        assertEquals(null, result)
    }

    @Test
    fun hentNorskFnrPaalisteavPinListeIngenNorske() {
        val list = listOf(
                PinItem(sektor = "03", land = "SE", identifikator = "00987654321", institusjonsnavn = "SE"),
                PinItem(sektor = "02", land = "DK", identifikator = "05467898321", institusjonsnavn = "DK")
        )
        val result = service.getFnrMedLandkodeNO(list)
        assertEquals(null, result)
    }

    @Test
    fun hentYtelseKravtypeTesterPaaP15000Alderpensjon() {
        val filepath = "src/test/resources/json/nav/P15000-NAV.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))

        assertTrue(validateJson(json))

        val response: ResponseEntity<String> = ResponseEntity(json, HttpStatus.OK)

        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                eq(null),
                ArgumentMatchers.eq(String::class.java))
        ).thenReturn(response)

        val result = service.hentFnrOgYtelseKravtype("1234567890","100001000010000")
        assertEquals("21712", result.fnr)
        assertEquals("01", result.krav?.type)
        assertEquals("2019-02-01", result.krav?.dato)
    }

    @Test
    fun hentYtelseKravtypeTesterPaaP15000Gjennlevende() {
        val filepath = "src/test/resources/json/nav/P15000Gjennlevende-NAV.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))

        val orgsed = mapJsonToAny(json, typeRefs<SED>())

        val response: ResponseEntity<String> = ResponseEntity(json, HttpStatus.OK)

        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                eq(null),
                ArgumentMatchers.eq(String::class.java))
        ).thenReturn(response)

        val result = service.hentFnrOgYtelseKravtype("1234567890","100001000010000")
        assertEquals("32712", result.fnr)
        assertEquals("02", result.krav?.type)
        assertEquals("2019-02-01", result.krav?.dato)

        JSONAssert.assertEquals(json, orgsed.toJson(), false)
    }

    @Test
    fun feilerVedHentingAvP2100GrunnetManglendeMapping() {
        val filepath = "src/test/resources/json/nav/P2100-NAV-unfin.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))

        val response: ResponseEntity<String> = ResponseEntity(json, HttpStatus.OK)

        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                eq(null),
                ArgumentMatchers.eq(String::class.java))
        ).thenReturn(response)

        assertThrows<FagmodulJsonIllegalArgumentException> {
            val result = service.hentFnrOgYtelseKravtype("1234567890","100001000010000")
        }
    }

    @Test
    fun hentYtelseKravtypeTesterPaaP15000FeilerVedUgyldigSED() {
        val filepath = "src/test/resources/json/nav/P9000-NAV.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))

        val response: ResponseEntity<String> = ResponseEntity(json, HttpStatus.OK)

        whenever(mockEuxrestTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                eq(null),
                ArgumentMatchers.eq(String::class.java))
        ).thenReturn(response)

        assertThrows<SedDokumentIkkeGyldigException> {
            service.hentFnrOgYtelseKravtype("1234567890", "100001000010000")
        }
    }

    @Test
    fun `Calling euxService getAvailableSEDonBuc returns BuC lists`() {
        var buc = "P_BUC_01"
        var expectedResponse = listOf("P2000")
        var generatedResponse = EuxService.getAvailableSedOnBuc (buc)
        assertEquals(generatedResponse, expectedResponse)

        buc = "P_BUC_06"
        expectedResponse = listOf("P5000", "P6000", "P7000", "P10000")
        generatedResponse = EuxService.getAvailableSedOnBuc(buc)
        assertEquals(generatedResponse, expectedResponse)
    }

    @Test
    fun `Calling euxService getAvailableSedOnBuc no input, return`() {
        val expected = "[ \"P2000\", \"P2100\", \"P2200\", \"P8000\", \"P5000\", \"P6000\", \"P7000\", \"P10000\", \"P14000\", \"P15000\" ]"
        val actual = EuxService.getAvailableSedOnBuc(null)
        assertEquals(expected, actual.toJson())
    }

    fun mockSedResponse(sedJson: String): ResponseEntity<String> {
        return ResponseEntity(sedJson, HttpStatus.OK)
    }

    fun getTestJsonFile(filename: String): String {
        val filepath = "src/test/resources/json/nav/${filename}"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))
        return json
    }

    @Test
    fun `gitt en gyldig liste av Institusjoner naar http url genereres saa generer en liste av mottakere som path param`() {
        val euxCaseId = "1234"
        val correlationId = 123456778
        val deltaker = listOf(InstitusjonItem("NO","NO:NAV02","NAV"), InstitusjonItem("SE", "SE:SE2", "SVER"))
        val builder = UriComponentsBuilder.fromPath("/buc/$euxCaseId/mottakere")
                .queryParam("KorrelasjonsId", correlationId)
                .build()
        val url = builder.toUriString() + service.convertListInstitusjonItemToString(deltaker)
        println(url)
        assertEquals("/buc/1234/mottakere?KorrelasjonsId=123456778&mottakere=NO:NAV02&mottakere=SE:SE2", url)
    }

    @Test
    fun `Test filter list av rinasak ta bort elementer av archived`() {
        val dummyList = listOf(
                Rinasak("723","P_BUC_01",null,"PO",null,"open"),
                Rinasak("2123","P_BUC_03",null,"PO",null,"open"),
                Rinasak("423","H_BUC_01",null,"PO",null,"archived"),
                Rinasak("234","P_BUC_06",null,"PO",null,"closed"),
                Rinasak("8423","P_BUC_07",null,"PO",null,"archived")
                )

        val result = service.getFilteredArchivedaRinasaker(dummyList)
        assertEquals(3, result.size)
        assertEquals("2123", result.first())

    }

    @Test
    fun `Test filter list av rinasak ta bort elementer av archived og ugyldige buc`() {
        val dummyList = listOf(
                Rinasak("723","FP_BUC_01",null,"PO",null,"open"),
                Rinasak("2123","H_BUC_02",null,"PO",null,"open"),
                Rinasak("423","P_BUC_01",null,"PO",null,"archived"),
                Rinasak("234","FF_BUC_01",null,"PO",null,"closed"),
                Rinasak("8423","FF_BUC_01",null,"PO",null,"archived"),
                Rinasak("8223","H_BUC_07",null,"PO",null,"open")
        )

        val result = service.getFilteredArchivedaRinasaker(dummyList)
        assertEquals(1, result.size)
        assertEquals("8223", result.first())

    }

    @Test
    fun `Test filter list av rinasak ta bort elementer av archived og ugyldige buc samt spesielle a og b bucer`() {
        val dummyList = listOf(
                Rinasak("723","M_BUC_03a",null,"PO",null,"open"),
                Rinasak("2123","H_BUC_02",null,"PO",null,"open"),
                Rinasak("423","P_BUC_01",null,"PO",null,"archived"),
                Rinasak("234","FF_BUC_01",null,"PO",null,"closed"),
                Rinasak("8423","M_BUC_02",null,"PO",null,"archived"),
                Rinasak("8223","M_BUC_03b",null,"PO",null,"open")
        )

        val result = service.getFilteredArchivedaRinasaker(dummyList)
        assertEquals(2, result.size)
        assertEquals("723", result.first())
        assertEquals("8223", result.last())

    }


    @Test
    fun `Tester og evaluerer om require statement blir oppfylt`() {
        assertThrows<IllegalArgumentException> { dummyRequirement(null, null) }
        assertTrue( dummyRequirement("grtg", null))
        assertTrue( dummyRequirement(null, "hhgi"))
        assertTrue( dummyRequirement("kufghj", "fjhgb"))
    }

    @Test
    fun `filter ut gyldig sed fra json sedDocument`() {
        val json = String(Files.readAllBytes(Paths.get("src/test/resources/json/buc/buc-279020big.json")))
        val utils = BucUtils(mapJsonToAny(json, typeRefs()))

        val expected = listOf(Pair("04117b9f8374420e82a4d980a48df6b3","P2200"),
                Pair("eb938171a4cb4e658b3a6c011962d204","P5000"), Pair("3bc78059030444cda6d18a47ea1f0eec","P6000"),
                Pair("e418c061a4724f48b23e2191accf0cf6","P7000"), Pair("9fd0c413aa9d4f2f8cf394ea6e42abff","P8000"))
        val actual = service.filterUtGyldigSedId(utils.getAllDocuments().toJson())
        assertEquals(expected, actual)

    }



    @Test
    fun `filter ut gyldig sed fra json sedDocument tom liste`() {
        val expected = listOf<Pair<String,String>>()
        val actual = service.filterUtGyldigSedId("[]")
        assertEquals(expected, actual)
    }


    private fun dummyRequirement(dummyparam1: String?, dummyparam2: String?): Boolean{
        require(!(dummyparam1 == null && dummyparam2 == null)) { "Minst et søkekriterie må fylles ut for å få et resultat fra Rinasaker" }
        return true
    }
}
