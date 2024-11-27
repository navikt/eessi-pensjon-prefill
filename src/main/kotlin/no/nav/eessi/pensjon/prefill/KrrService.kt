package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.prefill.models.KrrPerson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Service
class KrrService(private val krrRestTemplate: RestTemplate,
                 @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest()) {

    private val logger: Logger = LoggerFactory.getLogger(KrrService::class.java)

    private lateinit var HentPerson: MetricsHelper.Metric

    init {
        HentPerson = metricsHelper.init("HentPerson", ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST))
    }

    //Henter inn telefonnummer og epostadresse fra KRR for å preutfylle SED
    fun hentPersonFraKrr(personIdent: String, inkluderSikkerDigitalPost: Boolean?= false): KrrPerson {
        return HentPerson.measure {
            val url = "/rest/v1/person?inkluderSikkerDigitalPost=$inkluderSikkerDigitalPost"
            logger.debug("Henter informasjon fra KRR: $url")

            val headers = HttpHeaders()
            headers["Nav-Personident"] = personIdent
            headers.contentType = MediaType.APPLICATION_JSON
            val httpEntity = HttpEntity("", headers)

            try {
                val response = krrRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    httpEntity,
                    String::class.java
                )

                logger.debug("Hent person fra KRR: response: ${response.body}".trimMargin())

                response.body?.let {
                    mapJsonToAny<KrrPerson>(it)
                } ?: throw IllegalArgumentException("Mangler melding fra KRR")
            } catch (e: HttpClientErrorException.NotFound) {
                throw IllegalArgumentException("Person: $personIdent ikke funnet (404)")
            } catch (e: Exception) {
                throw e
            }
        }

    }
}