package no.nav.eessi.pensjon

import com.fasterxml.jackson.databind.ser.std.StringSerializer
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkBeans
import no.nav.eessi.pensjon.config.RestTemplateConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.producer.ProducerConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [RestTemplateConfig::class, UnsecuredWebMvcTestLauncher::class, EessiApplicationConfigTest.KafkaConfig::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["excludeKodeverk","unsecured-webmvctest"])
@DirtiesContext
@EmbeddedKafka
@EnableMockOAuth2Server
@AutoConfigureMockMvc
@MockkBeans(
    MockkBean(name = "pdlRestTemplate", classes = [RestTemplate::class]),
    MockkBean(name = "kodeverkClient", classes = [KodeverkClient::class], relaxed = true)
)
class EessiApplicationConfigTest {

    @TestConfiguration
    class KafkaConfig {
        @Bean
        fun producerFactory(): ProducerFactory<String, String> {
            val configProps: MutableMap<String, Any> = HashMap<String, Any>()
            configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
            configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            return DefaultKafkaProducerFactory(configProps)
        }

        @Bean
        fun kafkaTemplate(): KafkaTemplate<String, String> {
            return KafkaTemplate(producerFactory())
        }
    }

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Test
    fun `contextTest`(){
        //alt er vel om vi kommer hit
    }
}
