package no.nav.eessi.eessifagmodul.services

import com.google.common.collect.Lists
import no.nav.eessi.eessifagmodul.models.Institusjon
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.web.client.RestTemplate

@RunWith(MockitoJUnitRunner::class)
class InstitutionServiceTest {

    @InjectMocks
    lateinit var service : InstitutionService

    @Mock
    lateinit var mockrestTemp : RestTemplate

    @Test
    fun testOnGettingInstitutionByID()  {
        val expected : Institusjon = Institusjon("SE", "Sverige")

        `when`(mockrestTemp.getForObject (anyString(),eq(expected::class.java))).thenReturn(expected)
        service.restTemplate = mockrestTemp

        val res = service.getInstitutionByID("23")
        Assert.assertNotNull(res)

        val result : Institusjon = res!!
        Assert.assertEquals(expected, result)
        Assert.assertEquals(expected.landkode, result.landkode)
        Assert.assertEquals("SE", result.landkode)
    }

    @Test
    fun testAllInstitutionByList() {
        val expected = Lists.newArrayList(Institusjon("SE","Sverige"), Institusjon("DK","Danmark"),Institusjon("FI","Finland"))

        val type : List<Institusjon> = Lists.newArrayList()

        `when`(mockrestTemp.getForObject (anyString(),eq(type::class.java))).thenReturn(expected)
        service.restTemplate = mockrestTemp

        val res : List<Institusjon>? = service.getAllInstitutions()
        Assert.assertNotNull(res)

        val result : List<Institusjon> = res!!
        Assert.assertEquals(expected, result)
        Assert.assertEquals(3, result.size)
    }

}
