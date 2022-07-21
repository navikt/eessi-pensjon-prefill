package no.nav.eessi.pensjon.prefill

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjoninformasjonException
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjonsinformasjonClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [IntegrasjonsTestConfig::class, UnsecuredWebMvcTestLauncher::class, PensjonsinformasjonServiceIT.TestConfig::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["unsecured-webmvctest"])
@AutoConfigureMockMvc
@EmbeddedKafka
class PensjonsinformasjonServiceIT {

    @Autowired
    private lateinit var pensjoninformasjonRestTemplate : RestTemplate

    @Autowired
    private lateinit var pensjonsinformasjonClient: PensjonsinformasjonClient

    @Autowired
    private lateinit var pensjonsinformasjonService: PensjonsinformasjonService

    @TestConfiguration
    internal class TestConfig{
        @Bean
        @Primary
        fun restTemplate(): RestTemplate = mockk()
    }

    @Test
    fun  `Gitt et fnr og aktørid så skal det returneres en brukersaksliste ved henting av pensjonsinformasjon`() {
        val fnr = "220366234444"
        val aktorId = "4324235242"

        val generatedResponse =  javaClass.getResource("/pensjonsinformasjon/full-generated-response-with-brukersaksliste.xml")!!.readText()

        every { pensjoninformasjonRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns ResponseEntity(generatedResponse,  HttpStatus.OK)
        val bruksersaksliste = pensjonsinformasjonService.hentPensjonInformasjon(fnr, aktorId)
        assert(bruksersaksliste.brukersSakerListe.brukersSakerListe.size > 0)
    }

    @Test
    fun  `Gitt et fnr og aktørid så skal det kastes en PensjoninformasjonException når responsen er tom`() {
        val fnr = "220366234444"
        val aktorId = "4324235242"

        val generatedResponse =  javaClass.getResource("/pensjonsinformasjon/full-generated-response.xml")!!.readText()

        every { pensjoninformasjonRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns ResponseEntity(generatedResponse,  HttpStatus.OK)

        assertThrows<PensjoninformasjonException> {
            pensjonsinformasjonService.hentPensjonInformasjon(fnr, aktorId)
        }
    }
}