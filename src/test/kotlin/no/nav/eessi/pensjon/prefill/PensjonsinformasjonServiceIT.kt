package no.nav.eessi.pensjon.prefill

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkBeans
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjoninformasjonException
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjonsinformasjonClient
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.retry.annotation.EnableRetry
import org.springframework.stereotype.Component
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [
    IntegrasjonsTestConfig::class,
    PensjonsInfoRetryLogger::class,
    TestPensjonsInfoRetryConfig::class,
    UnsecuredWebMvcTestLauncher::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(profiles = ["retryConfigOverride","unsecured-webmvctest", "excludeKodeverk"])
@EnableRetry
@AutoConfigureMockMvc
@EmbeddedKafka
@MockkBeans(
    MockkBean(name = "kodeverkClient", classes = [KodeverkClient::class]),
    MockkBean(name = "restTemplate", classes = [RestTemplate::class])
)
class PensjonsinformasjonServiceIT {

    @Autowired
    private lateinit var pensjoninformasjonRestTemplate : RestTemplate

    @Autowired
    private lateinit var pensjonsinformasjonService: PensjonsinformasjonService

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

    @Test
    fun `Gitt et kall mot hentPensjonInformasjon, som kaster et exception, så skal dette gi 3 retry`(){
        assertThrows<PensjoninformasjonException> {
            pensjonsinformasjonService.hentPensjonInformasjon("12345", "112233")
        }
        verify(exactly = 3) { pensjoninformasjonRestTemplate.exchange("/fnr", any(), any<HttpEntity<Unit>>(), eq(String::class.java)) }
    }
}

@Profile("retryConfigOverride")
@Component("pensjonsInfoRetryConfig")
data class TestPensjonsInfoRetryConfig(val initialRetryMillis: Long = 10L)
