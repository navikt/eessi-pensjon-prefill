package no.nav.eessi.eessifagmodul.services

import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDResponse
import no.nav.eessi.eessifagmodul.models.PENBrukerData
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestTemplate
import java.util.*
import org.mockito.ArgumentMatchers.anyString


@RunWith(MockitoJUnitRunner::class)
class EESSIKomponentenServiceTest {

    @Bean
    fun restTemplageMock(templateBuilder: RestTemplateBuilder): RestTemplate {
        println("Mock template")
        println("Mock template")
        println("Mock template")
        return templateBuilder.rootUri("http://localhost:8080/").build()
    }

    @InjectMocks
    lateinit var kompService : EESSIKomponentenService

    @Mock
    lateinit var mockrestTemp : RestTemplate

    @Test
    fun testRequestAndResponse() {

        val data = PENBrukerData("12345678", "DummyTester", "12345678")
        val uuid = UUID.randomUUID()
        val response = OpprettBuCogSEDResponse(uuid, "RINA-12345678", "Statusen er nå")

        `when`(mockrestTemp.postForObject(anyString(), any(), eq(response::class.java))).thenReturn(response)
        kompService.restTemplate = mockrestTemp

        val res = kompService.opprettBuCogSED(data)

        Assert.assertNotNull(res)

        val resp : OpprettBuCogSEDResponse = res!!
        Assert.assertEquals(response.korrelasjonsID, resp.korrelasjonsID)

    }

}


