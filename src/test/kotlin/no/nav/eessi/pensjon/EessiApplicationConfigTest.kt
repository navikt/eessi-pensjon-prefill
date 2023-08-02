package no.nav.eessi.pensjon

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkBeans
import no.nav.eessi.pensjon.config.RestTemplateConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [RestTemplateConfig::class, UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["excludeKodeverk","unsecured-webmvctest"])
@EmbeddedKafka
@EnableMockOAuth2Server
@AutoConfigureMockMvc
@MockkBeans(
    MockkBean(name = "pdlRestTemplate", classes = [RestTemplate::class]),
    MockkBean(name = "kodeverkClient", classes = [KodeverkClient::class], relaxed = true)
)
class EessiApplicationConfigTest {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Test
    fun `contextTest`(){
        //alt er vel om vi kommer hit
    }
}
