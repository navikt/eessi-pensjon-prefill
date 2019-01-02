package no.nav.eessi.eessifagmodul.services.aktoerregister

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.util.*

private val logger = LoggerFactory.getLogger(AktoerregisterService::class.java)

data class Identinfo(
        val ident: String,
        val identgruppe: String,
        val gjeldende: Boolean
)

data class IdentinfoForAktoer(
        val identer: List<Identinfo>?,
        val feilmelding: String?
)

class AktoerregisterException(message: String) : RuntimeException(message)

@Service
class AktoerregisterService(val aktoerregisterOidcRestTemplate: RestTemplate) {

    private val AKTOERREGISTER_TELLER_NAVN = "eessipensjon_fagmodul.aktoerregister"
    private val AKTOERREGISTER_TELLER_TYPE_VELLYKKEDE = counter(AKTOERREGISTER_TELLER_NAVN, "vellykkede")
    private val AKTOERREGISTER_TELLER_TYPE_FEILEDE = counter(AKTOERREGISTER_TELLER_NAVN, "feilede")

    fun counter(name: String, type: String): Counter {
        return Metrics.counter(name, "type", type)
    }

    @Value("\${app.name}")
    lateinit var appName: String

    fun hentGjeldendeNorskIdentForAktorId(aktorid: String): String {
        val response = doRequest(aktorid, "NorskIdent")
        validateResponse(aktorid, response)
        return response[aktorid]!!.identer!![0].ident
    }

    fun hentGjeldendeAktorIdForNorskIdent(norskIdent: String): String {
        val response = doRequest(norskIdent, "AktoerId")
        validateResponse(norskIdent, response)
        return response[norskIdent]!!.identer!![0].ident
    }

    private fun validateResponse(aktorid: String, response: Map<String, IdentinfoForAktoer>) {
        if (response[aktorid] == null)
            throw RuntimeException("Ingen identinfo for $aktorid ble funnet")

        val identInfoForAktoer = response[aktorid]!!

        if (identInfoForAktoer.feilmelding != null)
            throw AktoerregisterException(identInfoForAktoer.feilmelding)

        if (identInfoForAktoer.identer == null || identInfoForAktoer.identer.isEmpty())
            throw RuntimeException("Ingen identer returnert for $aktorid")

        if (identInfoForAktoer.identer.size > 1) {
            logger.debug("Identer returnert fra api:")
            identInfoForAktoer.identer.forEach {
                logger.debug("ident: ${it.ident}, gjeldende: ${it.gjeldende}, identgruppe: ${it.identgruppe}")
            }
            throw RuntimeException("Forventet 1 ident, fant ${identInfoForAktoer.identer.size}")
        }
    }

    private fun doRequest(ident: String, identGruppe: String, gjeldende: Boolean = true): Map<String, IdentinfoForAktoer> {
        val headers = HttpHeaders()
        headers["Nav-Personidenter"] = ident
        headers["Nav-Consumer-Id"] = appName
        headers["Nav-Call-Id"] = UUID.randomUUID().toString()
        val requestEntity = HttpEntity<String>(headers)

        val uriBuilder = UriComponentsBuilder.fromPath("/identer")
                .queryParam("identgruppe", identGruppe)
                .queryParam("gjeldende", gjeldende)

        val responseEntity = aktoerregisterOidcRestTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, requestEntity, String::class.java)

        if (responseEntity.statusCode.isError) {
            logger.error("Received ${responseEntity.statusCode} from aktørregister")
            AKTOERREGISTER_TELLER_TYPE_FEILEDE.increment()
            if (responseEntity.hasBody()) {
                logger.error(responseEntity.body.toString())
            }
            throw RuntimeException("Received ${responseEntity.statusCode} ${responseEntity.statusCode.reasonPhrase} from aktørregisteret")
        }
        AKTOERREGISTER_TELLER_TYPE_VELLYKKEDE.increment()

        return jacksonObjectMapper().readValue(responseEntity.body!!)
    }
}
