package no.nav.eessi.eessifagmodul.controllers

import com.google.common.collect.Lists
import no.nav.eessi.eessifagmodul.models.Institusjon
import no.nav.eessi.eessifagmodul.services.InstitutionService
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class InstitutionControllerTest {

    @InjectMocks
    lateinit var institutionController : InstitutionController

    @Mock
    lateinit var mockService : InstitutionService


    @Test
    fun testInstitutionsById() {
        val expected = Institusjon("SE", "Sverige")
        `when`(mockService.getInstitutionByID("DK")).thenReturn(expected)
        institutionController.service = mockService

        val result = institutionController.getInstitutionsById("DK")
        assertNotNull(result)
        assertEquals(expected, result!!)
        assertEquals(expected.landkode, result.landkode)
        assertEquals(expected.navn, result.navn)
    }

    @Test
    fun testInstitutionsByTopic() {
        val topic = "TodyTopicIsGreen"
        val result = institutionController.getInstitutionsByTopic(topic)
        assertNotNull(result)
        val inst : Institusjon = if (result != null) result else throw NullPointerException("Expression 'result' must not be null")
        assertEquals(topic, inst.navn)
        assertEquals("SE", inst.landkode)
    }

    @Test
    fun testInstitutionsByTopicNullTopic() {
        val topic : String? = null
        val result = institutionController.getInstitutionsByTopic(topic)
        assertNull(result)
    }

    @Test
    fun testInstitutionsByCountry() {
        //`when`(mockrestTemp.getForObject (anyString(),eq(type::class.java))).thenReturn(expected)
        //service.restTemplate = mockrestTemp

        val countryID = "FI"
        val result = institutionController.getInstitutionsByCountry(countryID)
        assertNotNull(result)
        assertEquals(countryID, result.landkode)
        assertEquals("Sverige", result.navn)
    }
    
    @Test
    fun testAllInstitutions() {
        val expected = Lists.newArrayList(Institusjon("SE","Sverige"), Institusjon("DK","Danmark"),Institusjon("FI","Finland"))
        `when`(mockService.getAllInstitutions()).thenReturn(expected)
        institutionController.service = mockService

        val result = institutionController.getAllInstitutions()
        assertNotNull(result)
        val res : List<Institusjon> = result!!
        assertEquals(3, res.size)
        res.forEach { print(it) }
    }
}