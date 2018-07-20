package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.preutfyll.InstitusjonItem
import no.nav.eessi.eessifagmodul.preutfyll.PreutfyllingPerson
import no.nav.eessi.eessifagmodul.preutfyll.UtfyllingData
import no.nav.eessi.eessifagmodul.services.EuxService
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class ApiControllerTest {

    @Mock
    lateinit var mockEuxService: EuxService

    @Mock
    lateinit var mockPreutfyll: PreutfyllingPerson

    lateinit var apiController: ApiController

    @Before
    fun setUp() {
        apiController = ApiController(mockEuxService, mockPreutfyll)
    }

    @Test
    fun `create frontend request`() {
        val json = "{\"institutions\":[{\"country\":\"NO\",\"institution\":\"DUMMY\"}],\"buc\":\"P_BUC_06\",\"sed\":\"P6000\",\"caseId\":\"caseId\",\"actorId\":\"0105094340092\"}"
        //map json request back to FrontendRequest obj
        val map = jacksonObjectMapper()
        val req = map.readValue(json, ApiController.RequestApi::class.java)
        assertNotNull(req)
        assertEquals("P_BUC_06",req.buc)
        assertEquals("DUMMY", req.institutions!![0].institution)
    }

    @Test
    fun `create document`() {
        val requestMock = ApiController.RequestApi(
            subjectArea = "Pensjon",
            caseId = "EESSI-PEN-123",
            institutions = listOf(ApiController.Institusjon("NO","DUMMY")),
            sed = "P6000",
            buc = "P_BUC_06",
            pinid = "0105094340092"
        )
        val mockResponse = "1234567890"

        val utfyllMock = UtfyllingData().mapFromRequest("Pensjon","EESSI-PEN-123","P_BUC_06","P6000","0105094340092")

        whenever(mockPreutfyll.preutfyll(any())).thenReturn(utfyllMock.hentSED())
        whenever(mockEuxService.createCaseAndDocument(anyString(), anyString(), anyString(), anyString(), anyString(), anyString() )).thenReturn(mockResponse)

        val response = apiController.createDocument(requestMock)
        Assert.assertEquals("{\"euxcaseid\":\"$mockResponse\"}" , response)
    }

    @Test
    fun `confirm document`() {
        val mockData = ApiController.RequestApi(
                subjectArea = "Pensjon",
                caseId = "EESSI-PEN-123",
                institutions = listOf(ApiController.Institusjon("NO","DUMMY")),
                sed = "P6000",
                buc = "P_BUC_06",
                pinid = "0105094340092"
        )
        val utfyllMock = UtfyllingData().mapFromRequest(subject = "Pensjon",caseId = "EESSI-PEN-123", sedID = "P6000", aktoerID = "0105094340092", buc = "P_BUC_06")
        utfyllMock.hentSED().nav = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy")))

        utfyllMock.addInstitutions(InstitusjonItem(country = "NO", institution = "DUMMY"))

        whenever(mockPreutfyll.preutfyll(any())).thenReturn(utfyllMock.hentSED())

        val response = apiController.confirmDocument(mockData)

        assertNotNull(response)
        assertEquals("P6000", response.sed)

    }

    @Test(expected = IllegalArgumentException::class)
    fun `confirm document when sed is not valid`() {
        val mockData = ApiController.RequestApi(
                subjectArea = "Pensjon",
                caseId = "EESSI-PEN-123",
                institutions = listOf(ApiController.Institusjon("NO","DUMMY")),
                sed = "P3300",
                buc = "P_BUC_06",
                pinid = "0105094340092"
        )
        apiController.confirmDocument(mockData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `confirm document sed is null`() {
        val mockData = ApiController.RequestApi(
                subjectArea = "Pensjon",
                caseId = "EESSI-PEN-123",
                institutions = listOf(ApiController.Institusjon("NO","DUMMY")),
                sed = null,
                buc = "P_BUC_06",
                pinid = "0105094340092"
        )
        apiController.confirmDocument(mockData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `check on caseID is null`() {
        val mockData = ApiController.RequestApi(
                caseId = null,
                sed = "P6000",
                pinid = "0105094340092"
        )
        apiController.confirmDocument(mockData)
    }
    @Test(expected = IllegalArgumentException::class)
    fun `check on pinID is null`() {
        val mockData = ApiController.RequestApi(
                caseId = "1213123123",
                sed = "P6000",
                pinid = null
        )
        apiController.confirmDocument(mockData)
    }


}