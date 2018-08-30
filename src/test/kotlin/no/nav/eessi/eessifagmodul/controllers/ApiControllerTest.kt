package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.clients.aktoerid.AktoerIdClient
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.prefill.PrefillPerson
import no.nav.eessi.eessifagmodul.prefill.PrefillSED
import no.nav.eessi.eessifagmodul.services.RinaActions
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.eessi.eessifagmodul.services.LandkodeService
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.web.util.UriComponentsBuilder
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.util.HashMap



@RunWith(MockitoJUnitRunner::class)
class ApiControllerTest {

    @Mock
    lateinit var mockEuxService: EuxService

    @Mock
    lateinit var mockPersonPreutfyll: PrefillPerson

    @Mock
    private lateinit var mockAktoerIdClient: AktoerIdClient

    @Mock
    private lateinit var mockRinaActions: RinaActions

    private lateinit var prefillDataMock: PrefillDataModel

    private lateinit var apiController: ApiController

    @Before
    fun setUp() {
        prefillDataMock = PrefillDataModel(mockAktoerIdClient)
        mockRinaActions = RinaActions(mockEuxService)
        apiController = ApiController(mockEuxService, PrefillSED(mockPersonPreutfyll), prefillDataMock)
        apiController.landkodeService = LandkodeService()
        apiController.rinaActions = mockRinaActions
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
        assertEquals("P_BUC_06",req.buc)
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

        whenever(mockAktoerIdClient.hentPinIdentFraAktorid(ArgumentMatchers.anyString())).thenReturn("12345")

        val utfyllMock =  prefillDataMock.build(
                subject = requestMock.subjectArea!!,
                caseId = requestMock.caseId!!,
                sedID = requestMock.sed!!,
                aktoerID = requestMock.pinid!!,
                buc = requestMock.buc!!,
                institutions = requestMock.institutions!!
        )

        assertNotNull(utfyllMock.getPinid())
        assertEquals("12345", utfyllMock.getPinid())

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

        whenever(mockPersonPreutfyll.prefill(any() )).thenReturn(utfyllMock.getSED())
        whenever(mockEuxService.createCaseAndDocument(anyString(), anyString(), anyString(), anyString(), anyString(), anyString() )).thenReturn(mockResponse)

        whenever(mockEuxService.getPossibleActions(ArgumentMatchers.anyString())).thenReturn(mockAksjonlist)

        val response = apiController.createDocument(requestMock)
        Assert.assertEquals("{\"euxcaseid\":\"$mockResponse\"}" , response)
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

        whenever(mockAktoerIdClient.hentPinIdentFraAktorid(ArgumentMatchers.anyString())).thenReturn("12345")
        val utfyllMock =  prefillDataMock.build(
                subject = requestMock.subjectArea!!,
                caseId = requestMock.caseId!!,
                sedID = requestMock.sed!!,
                aktoerID = requestMock.pinid!!,
                buc = requestMock.buc!!,
                institutions = requestMock.institutions!!
        )
        assertNotNull(utfyllMock.getPinid())
        assertEquals("12345", utfyllMock.getPinid())

        val mockAksjonlist = listOf(
                RINAaksjoner(
                        navn = "Create",
                        id = "123123343123",
                        kategori = "Documents",
                        dokumentType = "P6000",
                        dokumentId = "213123123"
                )
        )
        whenever(mockPersonPreutfyll.prefill(any())).thenReturn(utfyllMock.getSED())
        whenever(mockEuxService.createCaseAndDocument(anyString(), anyString(), anyString(), anyString(), anyString(), anyString() )).thenReturn(mockResponse)
        whenever(mockEuxService.getPossibleActions(anyString())).thenReturn(mockAksjonlist)

        apiController.createDocument(requestMock)
    }


    @Test
    fun `confirm document`() {
        val mockData = ApiController.ApiRequest(
                subjectArea = "Pensjon",
                caseId = "EESSI-PEN-123",
                institutions = listOf(InstitusjonItem("NO","DUMMY")),
                sed = "P6000",
                buc = "P_BUC_06",
                pinid = "0105094340092"
        )
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        val utfyllMock = prefillDataMock.build(subject = "Pensjon",caseId = "EESSI-PEN-123", sedID = "P6000", aktoerID = "0105094340092", buc = "P_BUC_06", institutions = items)
        utfyllMock.getSED().nav = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy")))

        whenever(mockPersonPreutfyll.prefill(any() )).thenReturn(utfyllMock.getSED())

        val response = apiController.confirmDocument(mockData)

        assertNotNull(response)
        assertEquals("P6000", response.sed)

    }

    @Test(expected = IllegalArgumentException::class)
    fun `confirm document when sed is not valid`() {
        val mockData = ApiController.ApiRequest(
                subjectArea = "Pensjon",
                caseId = "EESSI-PEN-123",
                institutions = listOf(InstitusjonItem("NO","DUMMY")),
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
                institutions = listOf(InstitusjonItem("NO","DUMMY")),
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

        val uriParams = HashMap<String, String>()
        uriParams["rinanr"] = "123456789"
        uriParams["documentid"] = "DOC1223213234234"

        val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)


        val uristr = builder.toUriString()
        println(uristr)
        val uri = builder.toUri()
        println(uri)
        val path2 = builder.path
        println(path2)

        assertEquals("/sed/get/123456789/DOC1223213234234", builder.path)
    }


}