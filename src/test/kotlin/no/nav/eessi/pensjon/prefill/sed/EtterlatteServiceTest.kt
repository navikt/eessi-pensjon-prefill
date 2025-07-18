package no.nav.eessi.pensjon.prefill.sed

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.prefill.EtterlatteService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

class EtterlatteServiceTest {
    private lateinit var etterlatteService: EtterlatteService
    private lateinit var etterlatteRestTemplate: RestTemplate

    @BeforeEach
    fun setUp() {
        etterlatteRestTemplate = mockk<RestTemplate>()
        etterlatteService = EtterlatteService(etterlatteRestTemplate)
    }

    @Test
    fun `hentGjennyVedtak skal gi success naar den finner vedtak`() {
        val fnr = "12345678901"
        val responseBody = """
        {
          "vedtak": [
            {
              "sakId": 0,
              "sakType": "string",
              "virkningstidspunkt": "2025-01-01",
              "type": "INNVILGELSE",
              "utbetaling": [
                {
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-01-31",
                  "beloep": "string"
                }
              ],
              "iverksettelsesTidspunkt": "2025-07-18T14:23:45.123456Z"
            }
          ]
        }
            """.trimMargin()

        mockSuccessfulVedtakResponse(fnr, responseBody)

        val result = etterlatteService.hentGjennyVedtak()

        assertTrue(result.isSuccess)
        verifyRequestVedtakMadeOnce()
        assertNotNull(result.getOrNull())
        assertEquals(LocalDateTime.parse("2025-07-18T14:23:45.123456"),result.getOrNull()?.vedtak?.firstOrNull()?.iverksettelsesTidspunkt)
    }

    private fun mockSuccessfulVedtakResponse(fnr: String, responseBody: String) {
        val url = buildVedtakUrl()
        val responseEntity = ResponseEntity(responseBody, HttpStatus.OK)

        every {
            etterlatteRestTemplate.exchange(
                url,
                HttpMethod.POST,
                any<HttpEntity<String>>(),
                String::class.java
            )
        } returns responseEntity
    }

    private fun verifyRequestVedtakMadeOnce() {
        val url = buildVedtakUrl()
        verify(exactly = 1) {
            etterlatteRestTemplate.exchange(
                url,
                HttpMethod.POST,
                any<HttpEntity<String>>(),
                String::class.java
            )
        }
    }

    private fun buildVedtakUrl(): String {
        return "/api/v1/vedtak"
    }
}