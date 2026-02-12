import no.nav.eessi.pensjon.prefill.PesysService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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

    @Nested
    inner class HentP2000Verdier{

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

        @Test
        fun `hentP2000data mapper alle verdier fra p2000-alder json til P2xxxMeldingOmPensjonDto`() {
            val p2000Json = javaClass.getResource("/pesys-endepunkt-2026/p2000-alder.json")!!.readText()
            server.expect(requestTo("/sed/p2000"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist("vedtakId"))
                .andExpect(header("fnr", "456"))
                .andExpect(header("sakId", "789"))
                .andRespond(withSuccess(p2000Json, MediaType.APPLICATION_JSON))

            val result = pesysService.hentP2000data("", "456", "789")
            // vedtak
            assert(result?.vedtak?.boddArbeidetUtland == true)
            // sak
            assert(result?.sak?.sakType?.name == "ALDER")
            assert(result?.sak?.forsteVirkningstidspunkt.toString() == "2025-01-01")
            assert(result?.sak?.status?.name == "TIL_BEHANDLING")
            // kravHistorikk
            val kravHistorikk = result?.sak?.kravHistorikk
            assert(kravHistorikk?.size == 11)
            assert(kravHistorikk?.first()?.kravId == "49256020")
            assert(kravHistorikk?.get(4)?.kravStatus?.name == "EESSI_AVBRUTT")
            assert(kravHistorikk?.get(10)?.kravAarsak?.name == "ANNEN_ARSAK")
            // ytelsePerMaaned
            val ytelsePerMaaned = result?.sak?.ytelsePerMaaned
            assert(ytelsePerMaaned?.size == 2)
            assert(ytelsePerMaaned?.first()?.belop == 5057)
            assert(ytelsePerMaaned?.get(1)?.ytelseskomponent?.get(2)?.ytelsesKomponentType == "TP")
            assert(ytelsePerMaaned?.get(1)?.ytelseskomponent?.get(2)?.belopTilUtbetaling == 2160)
            server.verify()
        }
    }
    @Nested
    inner class HentP2100Verdier{

    }

    @Nested
    inner class HentP2200Verdier{

    }

}