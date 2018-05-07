package no.nav.eessi.eessifagmodul.controllers

import com.google.common.collect.Lists
import no.nav.eessi.eessifagmodul.models.Institusjon
import no.nav.eessi.eessifagmodul.services.InstitutionService
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
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

    @Test
    fun getInstitutionsById() {

        val expected = Institusjon("SE", "Sverige")
        val mockService : InstitutionService =  mock(InstitutionService::class.java)
        `when`(mockService.getInstitutionByID("2")).thenReturn(expected)
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
        val result = testRestTemplate.getForEntity("$path$datapth", Institusjon::class.java)
        println(result)

        Assert.assertNotNull(result)
        Assert.assertEquals(true, result.hasBody())
        Assert.assertEquals(ResponseEntity::class.java, result::class.java)
        Assert.assertNotNull(result.body)

        val body: Institusjon = result.body!!
        Assert.assertEquals(Institusjon::class.java, body::class.java)
        Assert.assertEquals("SE", body.landkode)
        Assert.assertEquals("Finland", body.navn)
    }

    @Test
    fun getAllInstitutions() {

        val vars = Lists.newArrayList(Institusjon("SE", "Sverige"), Institusjon("DK", "Danmark"), Institusjon("FI", "Finland"))
        val mockService : InstitutionService =  mock(InstitutionService::class.java)
        `when`(mockService.getAllInstitutions()).thenReturn(vars)
        institutionController.service = mockService

        val result = testRestTemplate.getForEntity("$path/all", vars::class.java, vars)
        println(result)

        Assert.assertNotNull(result)
        Assert.assertEquals(true, result.hasBody())
        Assert.assertEquals(ResponseEntity::class.java, result::class.java)
        Assert.assertNotNull(result.body)
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

        Assert.assertNotNull(result)
        Assert.assertEquals(true, result.hasBody())
        Assert.assertEquals(ResponseEntity::class.java, result::class.java)
        Assert.assertNotNull(result.body)

        val body: Institusjon = result.body!!
        Assert.assertEquals(Institusjon::class.java, body::class.java)
        Assert.assertEquals("DK", body.landkode)
        Assert.assertEquals("Sverige", body.navn)
    }
}
