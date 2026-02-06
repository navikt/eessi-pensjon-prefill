import no.nav.eessi.pensjon.prefill.PesysService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate

@Disabled("Testen er ikke ferdig implementert, og må oppdateres for å bruke MockRestServiceServer i stedet for mockk")
class PesysServiceTest {

//    private val pesysService = PesysService(mockk(relaxed = true))

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

//        assertNull(result)
        server.verify()
    }

    @Test
    fun `buildUri legger til alle parameter`() {
//        val method = pesysService.javaClass.getDeclaredMethod(
//            "buildUri",
//            String::class.java,
//            Array<Any>::class.java
//        ).apply { isAccessible = true }

//            val uri = pesysService.buildUri(
//            "/sed/p2000",
//            arrayOf(
//                "vedtaksId" to "123",
//                "fnr" to "456",
//                "sakId" to "789"
//            )
//        )
//
//        assertEquals("/sed/p2000?vedtaksId=123&fnr=456&sakId=789", uri.toString())
    }

//    @Test
//    fun `leggTilParameter filterer null eller tommer parameter`() {
//        val url = pesysService.javaClass.getDeclaredMethod(
//            "get", String::class.java, List::class.java
//        ).apply { isAccessible = true }
//            .invoke(pesysService, "/sed/p6000", listOf(
//                "vedtaksId" to null,
//                "sakId" to ""
//            )) as String
//
//        assertEquals("/sed/p6000", url)
//    }
}