package no.nav.eessi.eessifagmodul.services

import com.google.common.collect.Lists
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.Institusjon
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner::class)
@ActiveProfiles("test")
class InstitutionServiceTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(InstitutionServiceTest::class.java)}

    @InjectMocks
    lateinit var service : InstitutionService

    @Autowired
    lateinit var eessiRest : EESSIRest

    @Mock
    lateinit var mockrestTemp : RestTemplate

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testOnGettingInstitutionByID()  {
        //fake data
        val expected = Institusjon("SE", "Sverige")
        //url path (rooturl + path)
        val testpath = "/cpi/getInstitution/23"

        //fake request
        val requestEntity = eessiRest.createGet(testpath)
        //fake response
        val responeEntity : ResponseEntity<Institusjon> = ResponseEntity(expected, HttpStatus.OK)

        //mock (restTemplate)
       `when`(mockrestTemp.exchange(eq(requestEntity),eq(eessiRest.typeRef<Institusjon>()))).thenReturn(responeEntity)

        //mock tilbake til helperbean
        eessiRest.restTemplate = mockrestTemp
        //helperbean settes til service
        service.rest = eessiRest

        //validate mock and service
        assertEquals(mockrestTemp, eessiRest.getRest())
        assertEquals(requestEntity, eessiRest.createGet(testpath))

        //prøver å kjøre selve funksjonen (hente fra 'basis')
        val res = service.getInstitutionByID("23")

        assertNotNull(res)
        assertEquals(HttpStatus.OK, res.statusCode)

        //hente ut object Institusjon
        val result : Institusjon = res.body!!

        //testvalidate - assert
        assertEquals(expected, result)
        assertEquals(expected::class.java, result::class.java)
        assertEquals(expected.landkode, result.landkode)
        assertEquals("SE", result.landkode)
        assertEquals("Sverige", result.navn)
    }

    @Test
    fun testAllInstitutionByList() {
        val testpath = "/cpi/getInstitutions"
        val expected = Lists.newArrayList(Institusjon("SE","Sverige"), Institusjon("DK","Danmark"),Institusjon("FI","Finland"))
        val responseEntity : ResponseEntity<List<Institusjon>> = ResponseEntity(expected, HttpStatus.OK)
        val requestEntity = eessiRest.createGet("/cpi/getInstitutions")

        whenever(mockrestTemp.exchange(eq(requestEntity),eq(eessiRest.typeRef<List<Institusjon>>()))).thenReturn(responseEntity)

        //mockrestTemplate settes til aktive i eessiRest
        eessiRest.restTemplate = mockrestTemp
        //setter eessi helper til aktive i service
        service.rest = eessiRest

        //validate mock og service
        assertEquals(mockrestTemp, eessiRest.restTemplate)
        assertEquals(requestEntity, eessiRest.createGet(testpath))

        //call service restTemplate to 'basis' (mock)
        val response = service.getAllInstitutions()

        //check
        assertNotNull(response)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result : List<Institusjon> = response.body!!

        //testvalidate
        assertEquals(expected, result)
        assertEquals(3, result.size)
        assertEquals(Institusjon::class.java, result.get(1)::class.java)
        assertEquals("Danmark", result[1].navn)

    }

    @Test
    fun testInstitusjonByTopic() {
        //fake data
        val expected = Institusjon("SE", "Finland")
        //url path (rooturl + path)
        val testpath = "/cpi/getInstitution/bytopic/Finland"

        //fake request
        val requestEntity = eessiRest.createGet(testpath)
        //fake response
        val responeEntity : ResponseEntity<Institusjon> = ResponseEntity(expected, HttpStatus.OK)

        //mock (restTemplate)
        whenever(mockrestTemp.exchange(eq(requestEntity),eq(eessiRest.typeRef<Institusjon>()))).thenReturn(responeEntity)

        //mock tilbake til helperbean
        eessiRest.restTemplate = mockrestTemp
        //helperbean settes til service
        service.rest = eessiRest

        //validate mock and service
        assertEquals(mockrestTemp, eessiRest.getRest())
        assertEquals(requestEntity, eessiRest.createGet(testpath))

        //prøver å kjøre selve funksjonen (hente fra 'basis')
        val res = service.getInstitutionsByTopic("Finland")

        assertNotNull(res)
        assertEquals(HttpStatus.OK, res.statusCode)

        //hente ut object Institusjon
        val result : Institusjon = res.body!!
        //testvalidate - assert
        assertEquals(expected, result)
        assertEquals(expected::class.java, result::class.java)
        assertEquals(expected.landkode, result.landkode)
        assertEquals("SE", result.landkode)
        assertEquals("Finland", result.navn)
    }

    @Test(expected = IllegalArgumentException::class )
    fun testInstitusjonByTopicBlank() {
        //prøver å kjøre selve funksjonen (hente fra 'basis')
         service.getInstitutionsByTopic("")
    }

    @Test(expected = IllegalArgumentException::class )
    fun testInstitusjonByTopicNull() {
        //prøver å kjøre selve funksjonen (hente fra 'basis')
        val none : String? = null
        service.getInstitutionsByTopic(none)
    }

}
