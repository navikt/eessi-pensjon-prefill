package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.prefill.models.pensjon.P15000overfoeringAvPensjonssakerTilEessiDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P8000AnmodningOmTilleggsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
@Service
class PesysService(
    private val pesysClientRestTemplate: RestTemplate
) {

    private val logger: Logger = LoggerFactory.getLogger(PesysService::class.java)

    fun hentP2000data(vedtakId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? =
        getWithHeaders("/sed/p2000",
            "vedtakId" to vedtakId,
            "fnr" to fnr,
            "sakId" to sakId
        )

    fun hentP2100data(vedtakId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? =
        getWithHeaders("/sed/p21000",
            "vedtakId" to vedtakId,
            "fnr" to fnr,
            "sakId" to sakId
        )

    fun hentP2200data(vedtakId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? =
        getWithHeaders("/sed/p22000",
            "vedtakId" to vedtakId,
            "fnr" to fnr,
            "sakId" to sakId
        )

    fun hentP6000data(vedtakId: String?): P6000MeldingOmVedtakDto? =
        getWithHeaders("/sed/p6000",
            "vedtakId" to vedtakId
        )

    fun hentP8000data(sakId: String): P8000AnmodningOmTilleggsinformasjon? =
        getWithHeaders("/sed/p8000",
            "sakId" to sakId
        )

    fun hentP15000data(vedtakId: String?, sakId: String): P15000overfoeringAvPensjonssakerTilEessiDto? =
        getWithHeaders("/sed/p15000",
            "vedtakId" to vedtakId,
            "sakId" to sakId
        )


    private inline fun <reified T : Any> getWithHeaders(
        path: String,
        vararg headers: Pair<String, String?>
    ): T? {
        val httpHeaders = HttpHeaders().apply {
            headers
                .filter { !it.second.isNullOrBlank() }
                .forEach { (k, v) -> set(k, v) }
        }

        logger.debug("Henter pesys informasjon fra: $path (headers=${httpHeaders})")

        val entity = HttpEntity<Void>(httpHeaders)
        return pesysClientRestTemplate.exchange(path, HttpMethod.GET, entity, T::class.java).body
    }
}

