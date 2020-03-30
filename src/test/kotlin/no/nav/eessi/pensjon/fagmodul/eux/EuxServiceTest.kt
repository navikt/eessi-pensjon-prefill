package no.nav.eessi.pensjon.fagmodul.eux

import com.nhaarman.mockitokotlin2.*
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.Rinasak
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.utils.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.web.util.UriComponentsBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId

@ExtendWith(MockitoExtension::class)
class EuxServiceTest {

    private lateinit var service: EuxService

    @Mock
    private lateinit var euxKlient: EuxKlient


    @BeforeEach
    fun setup() {
        service = EuxService(euxKlient)
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


    @Test
    fun `forventer et korrekt navsed P6000 ved kall til getSedOnBucByDocumentId`() {
        val filepath = "src/test/resources/json/nav/P6000-NAV.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))

        val orgsed = SED.fromJson(json)

        whenever(euxKlient.getSedOnBucByDocumentIdAsJson(any(), any())).thenReturn(json)

        val result = service.getSedOnBucByDocumentId("12345678900", "0bb1ad15987741f1bbf45eba4f955e80")

        assertEquals(orgsed, result)
        assertEquals("P6000", result.sed)

    }

    @Test
    fun hentYtelseKravtypeTesterPaaP15000Alderpensjon() {
        val filepath = "src/test/resources/json/nav/P15000-NAV.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))

        assertTrue(validateJson(json))

        whenever(euxKlient.getSedOnBucByDocumentIdAsJson(any(), any())).thenReturn(json)
        whenever(euxKlient.getFnrMedLandkodeNO(any())).thenReturn("21712")


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

        whenever(euxKlient.getSedOnBucByDocumentIdAsJson(any(), any())).thenReturn(json)
        whenever(euxKlient.getFnrMedLandkodeNO(any())).thenReturn("32712")

        val orgsed = mapJsonToAny(json, typeRefs<SED>())
        JSONAssert.assertEquals(json, orgsed.toJson(), false)

        val result = service.hentFnrOgYtelseKravtype("1234567890","100001000010000")
        assertEquals("32712", result.fnr)
        assertEquals("02", result.krav?.type)
        assertEquals("2019-02-01", result.krav?.dato)

    }

    @Test
    fun feilerVedHentingAvP2100GrunnetManglendeMapping() {
        val filepath = "src/test/resources/json/nav/P2100-NAV-unfin.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))

        whenever(euxKlient.getSedOnBucByDocumentIdAsJson(any(), any())).thenReturn(json)

        assertThrows<FagmodulJsonIllegalArgumentException> {
            service.hentFnrOgYtelseKravtype("1234567890","100001000010000")
        }
    }

    @Test
    fun hentYtelseKravtypeTesterPaaP15000FeilerVedUgyldigSED() {
        val filepath = "src/test/resources/json/nav/P9000-NAV.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))

        whenever(euxKlient.getSedOnBucByDocumentIdAsJson(any(), any())).thenReturn(json)

        assertThrows<SedDokumentIkkeGyldigException> {
            service.hentFnrOgYtelseKravtype("1234567890", "100001000010000")
        }
    }

    @Test
    fun `Calling eux-rina-api to create BucSedAndView for Frontend all OK excpect valid json`() {
        val rinasakerjson = "src/test/resources/json/rinasaker/rinasaker_34567890111.json"
        val rinasakStr = String(Files.readAllBytes(Paths.get(rinasakerjson)))
        assertTrue(validateJson(rinasakStr))

        val orgRinasaker = mapJsonToAny(rinasakStr, typeRefs<List<Rinasak>>())

        val bucjson = "src/test/resources/json/buc/buc-158123_2_v4.1.json"
        val bucStr = String(Files.readAllBytes(Paths.get(bucjson)))
        assertTrue(validateJson(bucStr))

        doReturn(bucStr)
                .whenever(euxKlient)
                .getBucJson(any())

        val result = service.getBucAndSedView(listOf(rinasakStr))

        assertNotNull(result)
        assertEquals(6, orgRinasaker.size)
        assertEquals(1, result.size)

        val firstJson = result.first()
        assertEquals("158123", firstJson.caseId)

        var lastUpdate: Long = 0
        firstJson.lastUpdate?.let { lastUpdate = it }
        assertEquals("2019-05-20T16:35:34",  Instant.ofEpochMilli(lastUpdate).atZone(ZoneId.systemDefault()).toLocalDateTime().toString())
        assertEquals(18, firstJson.seds?.size)

        val json = firstJson.toJson()
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

        doReturn(bucStr)
                .whenever(euxKlient)
                .getBucJson(any())

        val firstJson = service.getSingleBucAndSedView("158123")

        assertEquals("158123", firstJson.caseId)
        var lastUpdate: Long = 0
        firstJson.lastUpdate?.let { lastUpdate = it }
        assertEquals("2019-05-20T16:35:34",  Instant.ofEpochMilli(lastUpdate).atZone(ZoneId.systemDefault()).toLocalDateTime().toString())
        assertEquals(18, firstJson.seds?.size)
    }

    @Test
    fun `Calling euxService getAvailableSEDonBuc returns BuC lists`() {
        var buc = "P_BUC_01"
        var expectedResponse = listOf("P2000")
        var generatedResponse = service.getAvailableSedOnBuc (buc)
        assertEquals(generatedResponse, expectedResponse)

        buc = "P_BUC_06"
        expectedResponse = listOf("P5000", "P6000", "P7000", "P10000")
        generatedResponse = service.getAvailableSedOnBuc(buc)
        assertEquals(generatedResponse, expectedResponse)
    }

    @Test
    fun `Calling euxService getAvailableSedOnBuc no input, return`() {
        val expected = "[ \"P2000\", \"P2100\", \"P2200\", \"P8000\", \"P5000\", \"P6000\", \"P7000\", \"P10000\", \"P14000\", \"P15000\" ]"
        val actual = service.getAvailableSedOnBuc(null)
        assertEquals(expected, actual.toJson())
    }

    fun getTestJsonFile(filename: String): String {
        val filepath = "src/test/resources/json/nav/${filename}"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))
        return json
    }
}
