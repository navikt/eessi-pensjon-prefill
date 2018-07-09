package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.FrontendRequest
import no.nav.eessi.eessifagmodul.models.Institusjon
import no.nav.eessi.eessifagmodul.models.SED
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
    fun `create frontend request`() {
        val json = "{\"institutions\":[{\"country\":\"NO\",\"institution\":\"DUMMY\"}],\"buc\":\"P_BUC_06\",\"sed\":\"P6000\",\"caseId\":\"caseId\"}"
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
            buc = "P_BUC_06"
        )
        val mockResponse = "1234567890"

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
                buc = "P_BUC_06"
        )
        val response = apiController.confirmDocument(mockData)
        assertNotNull(response)
        assertEquals("P6000", response.sed)

    }


}