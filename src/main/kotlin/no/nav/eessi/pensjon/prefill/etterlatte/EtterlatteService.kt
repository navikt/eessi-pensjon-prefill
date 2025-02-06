package no.nav.eessi.pensjon.prefill.etterlatte

import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.utils.mapJsonToAny
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

@Component
class EtterlatteService(private val etterlatteRestTemplate: RestTemplate, @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest()) {
    private val logger = LoggerFactory.getLogger(EtterlatteService::class.java)
    private val secureLog = LoggerFactory.getLogger("secureLog")

    private var henterVedtaksInfoFraGjenny: MetricsHelper.Metric = metricsHelper.init("henterVedtaksInfoFraGjenny")

    fun hentGjennySak(fnr: String): Result<EtterlatteResponse?> {
        val url = "/api/v1/vedtak/"
        secureLog.info("Henter vedtaksinformasjon fra gjenny: $url")
        return try {
            val response = etterlatteRestTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity<String>("\"foedselsnummer\": \"fnr\"", HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                }),
                String::class.java
            )

            logger.debug("Hent vedtaksinformasjon fra gjenny: response: ${response.body}".trimMargin())

            response.body?.let {
                Result.success(mapJsonToAny<EtterlatteResponse>(it))
            } ?: Result.failure(IllegalArgumentException("Mangler melding fra gjenny")) // Handle null body
        } catch (e: HttpClientErrorException.NotFound) {
            Result.failure(IllegalArgumentException("Vedtaksinformasjon ikke funnet (404)"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class EtterlatteResponse(
    val sakId: Int,
    val sakType: String? = null,
    val virkningstidspunkt: LocalDate? = null,
    val type: String? = null,
    val utbetaling: List<Utbetaling>? = null
)

data class Utbetaling (
    val periode: Perioden,
    val beloep: String
)

data class Perioden (
    val start: LocalDate,
    val end: LocalDate,
)