package no.nav.eessi.eessifagmodul.controllers

import com.google.common.collect.Lists
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.Institusjon
import no.nav.eessi.eessifagmodul.services.InstitutionService
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InstitutionControllerIntegrationTest {

    val path: String = "/institutions"

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    lateinit var institutionController : InstitutionController

    @Mock
    lateinit var mockService : InstitutionService

    @Test
    fun getInstitutionsById() {
        //val mockService : InstitutionService =  mock(InstitutionService::class.java)
        //`when`(mockService.getInstitutionByID("2")).thenReturn(response)

        val expected = Institusjon("SE", "Sverige")
        val response : ResponseEntity<Institusjon> = ResponseEntity<Institusjon>(expected, HttpStatus.OK)
        whenever(mockService.getInstitutionByID("2")).thenReturn(response)
        institutionController.service = mockService

        val result = testRestTemplate.getForEntity("$path/byid/2", Institusjon::class.java)
        println(result)

        Assert.assertNotNull(result)
        Assert.assertEquals(true, result.hasBody())
        Assert.assertEquals(ResponseEntity::class.java, result::class.java)
        Assert.assertNotNull(result.body)
        val body: Institusjon = result.body!!
        Assert.assertEquals(Institusjon::class.java, body::class.java)
        Assert.assertEquals("SE", body.landkode)
        Assert.assertEquals("Sverige", body.navn)

    }

    @Test
    fun getInstitutionsByTopic() {
        val datapth = "/bytopic/Finland"

        val expected = Institusjon("SE", "Finland")
        val response : ResponseEntity<Institusjon> = ResponseEntity<Institusjon>(expected, HttpStatus.OK)

        //mock service
        whenever(mockService.getInstitutionsByTopic("Finland")).thenReturn(response)
        //initlize mock to service
        institutionController.service = mockService

        assertEquals(mockService, institutionController.service)

        val result = testRestTemplate.getForEntity("$path$datapth", Institusjon::class.java)
        println(result)

        assertNotNull(result)
        assertEquals(true, result.hasBody())
        assertEquals(ResponseEntity::class.java, result::class.java)
        assertNotNull(result.body)

        val body: Institusjon = result.body!!
        assertEquals(Institusjon::class.java, body::class.java)
        assertEquals("SE", body.landkode)
        assertEquals("Finland", body.navn)
    }

    @Test
    fun getInstitutionsByTopicBlank() {
        val datapth = "/bytopic/"

        //mock data
        val expected = Institusjon("ERROR", "ERROR")
        //mock response
        val response : ResponseEntity<Institusjon> = ResponseEntity<Institusjon>(expected, HttpStatus.OK)

        //mock service
        whenever(mockService.getInstitutionsByTopic (any())).thenReturn(response)
        institutionController.service = mockService

        //check servie same as controller service
        assertEquals(mockService, institutionController.service)

        //run test
        val result = testRestTemplate.getForEntity("$path$datapth/''", Institusjon::class.java)
        println(result)

        //validate testdata
        assertNotNull(result)
        assertEquals(true, result.hasBody())
        assertEquals(ResponseEntity::class.java, result::class.java)
        assertNotNull(result.body)

        val body: Institusjon = result.body!!
        assertEquals(Institusjon::class.java, body::class.java)

        assertNotNull(body)
        assertNotNull(body.navn)
        assertNotNull(body.landkode)
    }

    @Test
    fun getAllInstitutions() {

        val vars = Lists.newArrayList(Institusjon("SE", "Sverige"), Institusjon("DK", "Danmark"), Institusjon("FI", "Finland"))
        val response : ResponseEntity<List<Institusjon>> = ResponseEntity<List<Institusjon>>(vars, HttpStatus.OK)

        whenever(mockService.getAllInstitutions()).thenReturn(response)
        institutionController.service = mockService

        val result = testRestTemplate.getForEntity("$path/", vars::class.java, vars)
        println(result)

        assertNotNull(result)
        assertEquals(true, result.hasBody())
        assertEquals(ResponseEntity::class.java, result::class.java)
        assertNotNull(result.body)
        try {
            val bodylist: List<Institusjon> = result.body!!
            assertEquals(vars.size, bodylist.size)
            assertTrue(true)
        } catch (ex: Exception) {
            ex.printStackTrace()
            fail("Not here!")
        }
    }

    @Test
    fun getInstitusjonByCountryID() {
        val result = testRestTemplate.getForEntity("$path/bycountry/DK", Institusjon::class.java)
        println(result)

        assertNotNull(result)
        assertEquals(true, result.hasBody())
        assertEquals(ResponseEntity::class.java, result::class.java)
        assertNotNull(result.body)

        val body: Institusjon = result.body!!
        Assert.assertEquals(Institusjon::class.java, body::class.java)
        Assert.assertEquals("DK", body.landkode)
        Assert.assertEquals("Sverige", body.navn)
    }
}
