package no.nav.eessi.pensjon.integrationtest.pesys

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.security.sts.STSService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate


@SpringBootTest(classes = [UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["unsecured-webmvctest"])
@AutoConfigureMockMvc
class PesysIntegrationSpringTest {

    @MockBean
    private lateinit var stsService: STSService

    @MockBean(name = "euxOidcRestTemplate")
    private lateinit var restTemplate: RestTemplate

    @MockBean
    private lateinit var kodeverkClient: KodeverkClient

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `henter kravPensjonutland fra P2200`() {

        val bucid = "998777"
        val sedid = "9374f5978c6a46709a3fc1484062d158"

        doReturn("SWE").whenever(kodeverkClient).finnLandkode3(any())

        //euxrest kall buc
        val buc05 = ResourceUtils.getFile("classpath:json/buc/BucResponseFraEUXMedX007.json").readText()
        val rinabucpath = "/buc/$bucid"
        doReturn( ResponseEntity.ok().body( buc05 ) ).whenever(restTemplate).exchange( eq(rinabucpath), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        //euxrest kall til p2200
        val sedurl = "/buc/$bucid/sed/$sedid"
        val sedP2200 = ResourceUtils.getFile("classpath:json/nav/P2200-NAV_FRA_SE.json").readText()
        doReturn( ResponseEntity.ok().body( sedP2200 ) ).whenever(restTemplate).exchange( eq(sedurl), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/pesys/hentKravUtland/$bucid")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
            {
              "errorMelding": null,
              "mottattDato": "2019-07-15",
              "iverksettelsesdato": "2019-07-15",
              "fremsattKravdato":"2019-04-01",              
              "uttaksgrad": "0",
              "vurdereTrygdeavtale": true,
              "personopplysninger": {
                "statsborgerskap": "SWE"
              },
              "utland": null,
              "sivilstand": null,
              "soknadFraLand": "SWE",
              "initiertAv": "BRUKER"
            }
        """.trimIndent()

        JSONAssert.assertEquals(response, validResponse, true)

    }



}