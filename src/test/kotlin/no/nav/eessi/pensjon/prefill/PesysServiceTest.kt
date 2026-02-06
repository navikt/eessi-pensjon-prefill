import no.nav.eessi.pensjon.prefill.PesysService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate

class PesysServiceTest {

    private lateinit var restTemplate: RestTemplate
    private lateinit var server: MockRestServiceServer
    private lateinit var pesysService: PesysService

    @BeforeEach
    fun setup() {
        restTemplate = RestTemplate()
        server = MockRestServiceServer.bindTo(restTemplate).build()
        pesysService = PesysService(restTemplate)
    }


    @Test
    fun `hentP2000data sender alle header-verdier`() {
        server.expect(requestTo("/sed/p2000"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("vedtakId", "123"))
            .andExpect(header("fnr", "456"))
            .andExpect(header("sakId", "789"))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON)) // empty body => null DTO

        val result = pesysService.hentP2000data("123", "456", "789")

        assertNull(result)
        server.verify()
    }

    @Test
    fun `hentP2000data sender ikke vedtakId header naar den er null`() {
        server.expect(requestTo("/sed/p2000"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(headerDoesNotExist("vedtakId"))
            .andExpect(header("fnr", "456"))
            .andExpect(header("sakId", "789"))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        val result = pesysService.hentP2000data(null, "456", "789")

        assertNull(result)
        server.verify()
    }
}