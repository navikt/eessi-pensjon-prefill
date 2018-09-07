package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.prefill.PrefillPerson
import no.nav.eessi.eessifagmodul.prefill.PrefillSED
import no.nav.eessi.eessifagmodul.services.aktoerregister.AktoerregisterService
import no.nav.eessi.eessifagmodul.services.eux.EuxService
import no.nav.eessi.eessifagmodul.services.LandkodeService
import no.nav.eessi.eessifagmodul.services.PrefillService
import no.nav.eessi.eessifagmodul.services.eux.RinaActions
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.web.util.UriComponentsBuilder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@RunWith(MockitoJUnitRunner::class)
class ApiControllerTest {

    @Mock
    lateinit var mockEuxService: EuxService

    @Mock
    lateinit var mockPersonPreutfyll: PrefillPerson

    @Mock
    private lateinit var mockAktoerregisterService: AktoerregisterService

    @Mock
    private lateinit var mockRinaActions: RinaActions

    @Mock
    private lateinit var mockPrefillService: PrefillService

    private lateinit var mockPrefillSED: PrefillSED

    private lateinit var prefillDataMock: PrefillDataModel

    private lateinit var apiController: ApiController

    @Before
    fun setUp() {
        prefillDataMock = PrefillDataModel()
        mockRinaActions = RinaActions(mockEuxService)
        mockRinaActions.waittime = 200
        mockPrefillSED = PrefillSED(mockPersonPreutfyll)

        mockPrefillService = PrefillService(mockEuxService, mockPrefillSED, mockRinaActions)

        apiController = ApiController(mockEuxService, mockPrefillService, mockAktoerregisterService)
        apiController.landkodeService = LandkodeService()
    }

    @Test
    fun `create list landkoder`() {
        val response = apiController.getLandKoder()
        assertNotNull(response)
        assertEquals(31, response.size)
    }

    @Test
    fun `create frontend request`() {
        val json = "{\"institutions\":[{\"country\":\"NO\",\"institution\":\"DUMMY\"}],\"buc\":\"P_BUC_06\",\"sed\":\"P6000\",\"caseId\":\"caseId\",\"actorId\":\"0105094340092\"}"
        //map json request back to FrontendRequest obj
        val map = jacksonObjectMapper()
        val req = map.readValue(json, ApiController.ApiRequest::class.java)
        assertNotNull(req)
        assertEquals("P_BUC_06", req.buc)
        assertEquals("DUMMY", req.institutions!![0].institution)
    }

    @Test
    fun `create document`() {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))

        val requestMock = ApiController.ApiRequest(
                subjectArea = "Pensjon",
                caseId = "EESSI-PEN-123",
                institutions = items,
                sed = "P6000",
                buc = "P_BUC_06",
                pinid = "0105094340092"
        )
        val mockResponse = "1234567890"

        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(ArgumentMatchers.anyString())).thenReturn("12345")

        val pinid = apiController.hentAktoerIdPin("0105094340092")

        val utfyllMock = prefillDataMock.build(
                subject = requestMock.subjectArea!!,
                caseId = requestMock.caseId!!,
                sedID = requestMock.sed!!,
                aktoerID = requestMock.pinid!!,
                pinID = pinid,
                buc = requestMock.buc!!,
                institutions = requestMock.institutions!!
        )

        assertNotNull(utfyllMock.personNr)
        assertEquals("12345", utfyllMock.personNr)

        val mockAksjonlist = listOf(
                RINAaksjoner(
                        navn = "Update",
                        id = "123123123123",
                        kategori = "Documents",
                        dokumentType = "P6000",
                        dokumentId = "23123123"
                ),
                RINAaksjoner(
                        navn = "Delete",
                        id = "123123343123",
                        kategori = "Documents",
                        dokumentType = "P6000",
                        dokumentId = "213123123"
                )
        )

        whenever(mockPersonPreutfyll.prefill(any())).thenReturn(utfyllMock.sed)
        whenever(mockEuxService.createCaseAndDocument(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(mockResponse)

        whenever(mockEuxService.getPossibleActions(ArgumentMatchers.anyString())).thenReturn(mockAksjonlist)

        val response = apiController.createDocument(requestMock)
        Assert.assertEquals("{\"euxcaseid\":\"$mockResponse\"}", response)
    }

    @Test(expected = SedDokumentIkkeOpprettetException::class)
    fun `create document fail on confirmUpdate`() {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))

        val requestMock = ApiController.ApiRequest(
                subjectArea = "Pensjon",
                caseId = "EESSI-PEN-123",
                institutions = items,
                sed = "P6000",
                buc = "P_BUC_06",
                pinid = "0105094340092"
        )
        val mockResponse = "1234567890"

        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(ArgumentMatchers.anyString())).thenReturn("12345")
        val pinid = apiController.hentAktoerIdPin("0105094340092")


        val utfyllMock = prefillDataMock.build(
                subject = requestMock.subjectArea!!,
                caseId = requestMock.caseId!!,
                sedID = requestMock.sed!!,
                aktoerID = requestMock.pinid!!,
                pinID = pinid,
                buc = requestMock.buc!!,
                institutions = requestMock.institutions!!
        )
        assertNotNull(utfyllMock.personNr)
        assertEquals("12345", utfyllMock.personNr)

        val mockAksjonlist = listOf(
                RINAaksjoner(
                        navn = "Create",
                        id = "123123343123",
                        kategori = "Documents",
                        dokumentType = "P6000",
                        dokumentId = "213123123"
                )
        )
        whenever(mockPersonPreutfyll.prefill(any())).thenReturn(utfyllMock.sed)
        whenever(mockEuxService.getPossibleActions(anyString())).thenReturn(mockAksjonlist)
        whenever(mockEuxService.createCaseAndDocument(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(mockResponse)

        apiController.createDocument(requestMock)
    }


    @Test
    fun `confirm document`() {
        val mockData = ApiController.ApiRequest(
                subjectArea = "Pensjon",
                caseId = "EESSI-PEN-123",
                institutions = listOf(InstitusjonItem("NO", "DUMMY")),
                sed = "P6000",
                buc = "P_BUC_06",
                pinid = "0105094340092"
        )
        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(ArgumentMatchers.anyString())).thenReturn("12345")
        val pinid = apiController.hentAktoerIdPin("0105094340092")

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        val utfyllMock = prefillDataMock.build(
                subject = "Pensjon",
                caseId = "EESSI-PEN-123",
                sedID = "P6000",
                aktoerID = "0105094340092",
                pinID = pinid,
                buc = "P_BUC_06",
                institutions = items
        )
        utfyllMock.sed.nav = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy")))

        whenever(mockPersonPreutfyll.prefill(any())).thenReturn(utfyllMock.sed)

        val response = apiController.confirmDocument(mockData)

        assertNotNull(response)
        assertEquals("P6000", response.sed)

    }

    @Test(expected = IllegalArgumentException::class)
    fun `confirm document when sed is not valid`() {
        val mockData = ApiController.ApiRequest(
                subjectArea = "Pensjon",
                caseId = "EESSI-PEN-123",
                institutions = listOf(InstitusjonItem("NO", "DUMMY")),
                sed = "P3300",
                buc = "P_BUC_06",
                pinid = "0105094340092"
        )
        apiController.confirmDocument(mockData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `confirm document sed is null`() {
        val mockData = ApiController.ApiRequest(
                subjectArea = "Pensjon",
                caseId = "EESSI-PEN-123",
                institutions = listOf(InstitusjonItem("NO", "DUMMY")),
                sed = null,
                buc = "P_BUC_06",
                pinid = "0105094340092"
        )
        apiController.confirmDocument(mockData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `check on caseID is null`() {
        val mockData = ApiController.ApiRequest(
                caseId = null,
                sed = "P6000",
                pinid = "0105094340092"
        )
        apiController.confirmDocument(mockData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `check on pinID is null`() {
        val mockData = ApiController.ApiRequest(
                caseId = "1213123123",
                sed = "P6000",
                pinid = null
        )
        apiController.confirmDocument(mockData)
    }

    @Test
    fun `check rest api path correct`() {
        val path = "/sed/get/{rinanr}/{documentid}"
        val uriParams = mapOf("rinanr" to "123456789", "documentid" to "DOC1223213234234")
        val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)
        assertEquals("/sed/get/123456789/DOC1223213234234", builder.path)
    }

}