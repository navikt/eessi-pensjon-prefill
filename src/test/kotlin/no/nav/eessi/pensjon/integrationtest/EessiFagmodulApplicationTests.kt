package no.nav.eessi.pensjon.integrationtest

import no.nav.eessi.pensjon.security.sts.STSService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

private const val SED_SENDT_TOPIC = "eessi-basis-sedSendt-v1"

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(count = 1, controlledShutdown = true, topics = [SED_SENDT_TOPIC])
class EessiFagmodulApplicationTests {

    @MockBean
    lateinit var stsService: STSService

    @Test
    fun contextLoads() {
    }
}
