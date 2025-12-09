package no.nav.eessi.pensjon.prefill

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkBeans
import io.mockk.every
import io.mockk.verify
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjoninformasjonException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.retry.annotation.EnableRetry
import org.springframework.stereotype.Component
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate

private const val FNR_VOKSEN = "220366234444"
private const val AKTORID = "4324235242"

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
        val generatedResponse =  javaClass.getResource("/pensjonsinformasjon/full-generated-response-with-brukersaksliste.xml")!!.readText()

        every { pensjoninformasjonRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns ResponseEntity(generatedResponse,  HttpStatus.OK)
        val bruksersaksliste = pensjonsinformasjonService.hentPensjonInformasjon(FNR_VOKSEN, AKTORID)
        assert(bruksersaksliste.brukersSakerListe.brukersSakerListe.isNotEmpty())
    }

    @Test
    fun  `Gitt et fnr og aktørid så skal det kastes en PensjoninformasjonException når responsen er tom`() {
        val generatedResponse =  javaClass.getResource("/pensjonsinformasjon/full-generated-response.xml")!!.readText()

        every { pensjoninformasjonRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns ResponseEntity(generatedResponse,  HttpStatus.OK)

        assertThrows<PensjoninformasjonException> {
            pensjonsinformasjonService.hentPensjonInformasjon(FNR_VOKSEN, AKTORID)
        }
    }

    @Test
    fun `Gitt et kall mot hentPensjonInformasjon, som kaster et exception, så skal dette gi 3 retry`(){
        assertThrows<PensjoninformasjonException> {
            pensjonsinformasjonService.hentPensjonInformasjon(FNR_VOKSEN, AKTORID)
        }
        verify(exactly = 3) { pensjoninformasjonRestTemplate.exchange("/fnr", any(), any<HttpEntity<Unit>>(), eq(String::class.java)) }
    }
}

@Profile("retryConfigOverride")
@Component("pensjonsInfoRetryConfig")
data class TestPensjonsInfoRetryConfig(val initialRetryMillis: Long = 10L)
