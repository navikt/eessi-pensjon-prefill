package no.nav.eessi.pensjon.prefill.etterlatte

import no.nav.eessi.pensjon.utils.mapJsonToAny
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class EtterlatteService(private val etterlatteRestTemplate: RestTemplate) {

    private val logger = LoggerFactory.getLogger(EtterlatteService::class.java)

    /**
     * Henter vedtak fra etterlatte-api
     * @param aktoerId aktørId for den som skal hentes
     */

    fun hentGjennyVedtak(fnr: String): Result<EtterlatteVedtakResponseData?> {
        val url = "/api/v1/vedtak"
        logger.debug("Henter informasjon fra gjenny: $url")
        val json = """
            {
                "foedselsnummer": "$fnr"
            }
        """.trimIndent()

        return try {
            val response = etterlatteRestTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity(json, HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }),
                String::class.java
            )

            logger.info("Hent sak fra gjenny: response: ${response.body}".trimMargin())

            response.body?.let {
                Result.success(mapJsonToAny<EtterlatteVedtakResponseData>(it))
            } ?: Result.failure(IllegalArgumentException("Mangler melding fra gjenny")) // Handle null body
        } catch (_: HttpClientErrorException.NotFound) {
            Result.failure(IllegalArgumentException("Vedtak ikke funnet (404)"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}