package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.FrontendRequest
import no.nav.eessi.eessifagmodul.services.EuxService
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class ApiControllerTest {

    @Mock
    lateinit var mockEuxService: EuxService

    lateinit var apiController: ApiController

    @Before
    fun setUp() {
        apiController = ApiController(mockEuxService)
    }


    @Test
    fun `check that cached getBucs() returns a list of buc-names`() {
        val mockData = listOf(
                "P_BUC_01",
                "P_BUC_02",
                "P_BUC_05",
                "P_BUC_06",
                "P_BUC_07")
        whenever(mockEuxService.getCachedBuCTypePerSekor()).thenReturn(mockData)

        val response = apiController.getBucs()

        Assert.assertEquals(mockData.size, response.size)
        Assert.assertTrue(response.containsAll(mockData))
    }

    @Test
    fun `get all SED-types`() {
        val mockData = listOf(
                "P2000",
                "P2200",
                "P5000")
        //whenever(mockEuxService.getAvailableSEDTypes("")).thenReturn(mockData)
        whenever(mockEuxService.getAvailableSEDonBuc("")).thenReturn(mockData)

        val response = apiController.getSeds("")

        Assert.assertEquals(response.size, mockData.size)
        Assert.assertTrue(response.containsAll(mockData))
    }

    @Test
    fun `get all SED-types for specific BUC`() {
        val mockData = listOf(
                "P2000",
                "P2200"
        )
        whenever(mockEuxService.getAvailableSEDonBuc("P_BUC_01")).thenReturn(mockData)

        val response = apiController.getSeds("P_BUC_01")

        Assert.assertEquals(response.size , mockData.size)
        Assert.assertTrue(response.containsAll(mockData))
    }

    @Test
    fun `get all institutions`() {
        val mockData = listOf(
                "NAV02","DUMMY"
        )
        whenever(mockEuxService.getCachedInstitusjoner()).thenReturn(mockData)

        //whenever(mockEuxService.getInstitusjoner("","")).thenReturn(mockData)

        val response = apiController.getInstitutions("","")
        Assert.assertEquals(response.size , mockData.size)
        Assert.assertTrue(response.containsAll(mockData))


    }

//    @Test
//    fun `get all institutions for specific BUC`() {
//
//    }

    @Test
    fun `create frontend request`() {
        val json = "{\"institution\":\"DUMMY\",\"buc\":\"P_BUC_06\",\"sed\":\"P6000\",\"caseId\":\"caseId\"}"

        //map json request back to FrontendRequest obj
        val map = jacksonObjectMapper()
        val req = map.readValue(json, FrontendRequest::class.java)
        assertNotNull(req)
        assertEquals("P_BUC_06",req.buc)
        assertEquals("DUMMY", req.institution)
    }

    @Test
    fun `create document`() {
        val mockData = FrontendRequest(
            caseId = "EESSI-PEN-123",
            institution = "DUMMY",
            sed = "P6000",
            buc = "P_BUC_06"
        )
        val mockResponse = "1234567890"

        whenever(mockEuxService.createCaseAndDocument(anyString(), anyString(), anyString(), anyString(), anyString(), anyString() )).thenReturn(mockResponse)

        val response = apiController.createDocument(mockData)

        Assert.assertEquals("{\"euxcaseid\":\"$mockResponse\"}" , response)
    }
}