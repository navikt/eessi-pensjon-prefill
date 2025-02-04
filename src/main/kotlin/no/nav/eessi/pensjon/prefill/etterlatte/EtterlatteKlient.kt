package no.nav.eessi.pensjon.prefill.etterlatte

import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.utils.mapJsonToAny
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException

@Component
class EtterlatteKlient(private val etterlatteRestTemplate: RestTemplate,
                       @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest()
) {
    private val logger: Logger by lazy { LoggerFactory.getLogger(EtterlatteKlient::class.java) }
    private lateinit var hentVedtakFraGhenny: MetricsHelper.Metric

    init {
        hentVedtakFraGhenny = metricsHelper.init("hent vedtak fra gjenny")
    }

    fun hentVedtakFraGjenny(fnr: String): String {
        val path = "/api/v1/vedtak"
        logger.debug("Forsøker å hente ut vedtaksinformasjon fra Gjenny for: $fnr")

        return try {
            logger.info("Henter vedtak for fnr fra gjenny")
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON

            etterlatteRestTemplate.exchange(
                path,
                HttpMethod.POST,
                HttpEntity(fnr, headers),
                String::class.java
            ).body!!

        } catch (statusCodeEx: HttpStatusCodeException) {
            logger.error(
                "En HttpStatusCodeException oppstod under henting av vedtaksinformasjon fra Gjenny",
                statusCodeEx.cause
            )
            val errorMessage = ResponseErrorData.from(statusCodeEx)
            throw ResponseStatusException(statusCodeEx.statusCode, errorMessage.message)

        } catch (cliErrEx: HttpClientErrorException) {
            logger.error(
                "En HttpClientErrorException oppstod under henting av vedtaksinformasjon fra Gjenny",
                cliErrEx.cause
            )
            val errorMessage = ResponseErrorData.from(cliErrEx)
            throw ResponseStatusException(cliErrEx.statusCode, errorMessage.message)

        } catch (ex: Exception) {
            logger.error("En feil oppstod under henting av vedtaksinformasjon fra Gjenny", ex)
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "En feil oppstod under henting av preutfylt SED ex: ${ex.message}"
            )
        }

    }

    data class ResponseErrorData(
        val timestamp: String,
        val status: Int,
        val error: String,
        val message: String,
        val path: String
    ) {
        companion object {
            fun from(hsce: HttpStatusCodeException): ResponseErrorData {
                return mapJsonToAny(hsce.responseBodyAsString)
            }

            fun fromJson(json: String): ResponseErrorData {
                return mapJsonToAny(json)
            }
        }

        override fun toString(): String {
            return message
        }
    }
}
