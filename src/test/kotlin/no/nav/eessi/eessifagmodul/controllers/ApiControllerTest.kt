package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.preutfyll.Preutfylling
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
    lateinit var mockPreutfyll: Preutfylling

    lateinit var apiController: ApiController

    @Before
    fun setUp() {
        apiController = ApiController(mockEuxService, mockPreutfyll)
    }

    @Test
    fun `create frontend request`() {
        val json = "{\"institutions\":[{\"country\":\"NO\",\"institution\":\"DUMMY\"}],\"buc\":\"P_BUC_06\",\"sed\":\"P6000\",\"caseId\":\"caseId\",\"pinid\":\"0105094340092\"}"
        //map json request back to FrontendRequest obj
        val map = jacksonObjectMapper()
        val req = map.readValue(json, FrontendRequest::class.java)
        assertNotNull(req)
        assertEquals("P_BUC_06",req.buc)
        assertEquals("DUMMY", req.institutions!![0].institution)
    }

    @Test
    fun `create document`() {
        val mockData = FrontendRequest(
            subjectArea = "Pensjon",
            caseId = "EESSI-PEN-123",
            institutions = listOf(Institusjon("NO","DUMMY")),
            sed = "P6000",
            buc = "P_BUC_06",
            pinid = "0105094340092"
        )
        val mockResponse = "1234567890"

        val utfyll = UtfyllingData(createSED("P6000"), mockData, "1234567890")

        whenever(mockPreutfyll.preutfylling(mockData)).thenReturn(utfyll)

        whenever(mockEuxService.createCaseAndDocument(anyString(), anyString(), anyString(), anyString(), anyString(), anyString() )).thenReturn(mockResponse)

        val response = apiController.createDocument(mockData)

        Assert.assertEquals("{\"euxcaseid\":\"$mockResponse\"}" , response)
    }

    @Test
    fun `confirm document`() {
        val mockData = FrontendRequest(
                subjectArea = "Pensjon",
                caseId = "EESSI-PEN-123",
                institutions = listOf(Institusjon("NO","DUMMY")),
                sed = "P6000",
                buc = "P_BUC_06",
                pinid = "0105094340092"
        )

        val sed = createSED("P6000")
        sed.nav = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy")))
        val utfyll = UtfyllingData(sed, mockData, "15094349254")

        whenever(mockPreutfyll.preutfylling(mockData)).thenReturn(utfyll)

        val response = apiController.confirmDocument(mockData)
        assertNotNull(response)
        assertEquals("P6000", response.sed)

    }


}