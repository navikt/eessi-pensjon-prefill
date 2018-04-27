package no.nav.eessi.eessifagmodul.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.eessifagmodul.models.PENBrukerData
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EESSIKomponentenServiceTest {

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @Test
    fun testRequest() {

        val result = testRestTemplate.getForEntity("/komponent/opprettEESSIreq", String::class.java).body
        Assert.assertNotNull(result)
        println(result)

        //var req : EESSIKomponentenService.OpprettBuCogSEDRequest = mapper.readValue(result, EESSIKomponentenService.OpprettBuCogSEDRequest::class.java)
        try {
            val mapper = jacksonObjectMapper()
            val request: EESSIKomponentenService.OpprettBuCogSEDRequest = mapper.readValue(result,EESSIKomponentenService.OpprettBuCogSEDRequest::class.java)
            Assert.assertNotNull(request)
            println(request)
        } catch(e: Exception) {
            //.printStackTrace()
        }
    }

}


