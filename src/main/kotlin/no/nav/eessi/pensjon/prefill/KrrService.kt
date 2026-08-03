package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfoBolk
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfoBolkRequestBody
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
    private var hentPersoner: MetricsHelper.Metric

    init {
        hentPersoner = metricsHelper.init("HentPerson", ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST))
    }

    private fun maskPersonIdent(value: String?): String? {
        return value?.replace(Regex("""\b\d{11}\b""")) { matchResult ->
            matchResult.value.take(4) + "*".repeat(matchResult.value.length - 4)
        }
    }

    //Henter inn telefonnummer og epostadresse fra KRR for å preutfylle SED
    fun hentPersonerFraKrr(personIdent: String, inkluderSikkerDigitalPost: Boolean?= false) : DigitalKontaktinfo? {
        return hentPersoner.measure {
            val url = "/rest/v1/personer?inkluderSikkerDigitalPost=false"
            logger.debug("Henter informasjon fra KRR: $url")

            val request = DigitalKontaktinfoBolkRequestBody(
                personidenter = listOf(personIdent)
            )

            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            val httpEntity = HttpEntity(request, headers)

            try {
                val response = krrRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpEntity,
                    String::class.java
                )

                logger.debug("Hent person fra KRR med nytt endepunkt: response: ${maskPersonIdent(response.body)}".trimMargin())

                return@measure response.body?.let { mapJsonToAny<DigitalKontaktinfoBolk>(it) }?.personer?.get(personIdent)
                    ?: throw IllegalArgumentException("Mangler melding fra KRR")
            } catch (e: HttpClientErrorException.NotFound) {
                logger.error("Person: ${maskPersonIdent(personIdent)} ikke funnet (404)")
            }
            catch (e: Exception) {
                logger.error("Feil ved henting av person fra KRR for personIdent: ${maskPersonIdent(personIdent)}", e)
            }
            logger.error("KRR: Fant ikke person for personIdent: ${maskPersonIdent(personIdent)}")
            null
        }
    }

}