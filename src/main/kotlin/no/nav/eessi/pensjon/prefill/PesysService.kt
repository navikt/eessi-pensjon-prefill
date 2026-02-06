package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.prefill.models.pensjon.P15000overfoeringAvPensjonssakerTilEessiDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P8000AnmodningOmTilleggsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class PesysService(
    private val pesysClientRestTemplate: RestTemplate
) {

    private val logger: Logger = LoggerFactory.getLogger(PesysService::class.java)

    fun hentP2000data(vedtaksId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? =
        get("/sed/p2000",
            "vedtakId" to vedtaksId,
            "fnr" to fnr,
            "sakId" to sakId
        )

    fun hentP2100data(vedtaksId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? =
        get("/sed/p21000",
            "vedtakId" to vedtaksId,
            "fnr" to fnr,
            "sakId" to sakId
        )

    fun hentP2200data(vedtaksId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? =
        get("/sed/p22000",
            "vedtakId" to vedtaksId,
            "fnr" to fnr,
            "sakId" to sakId
        )

    fun hentP6000data(vedtaksId: String?): P6000MeldingOmVedtakDto? =
        get("/sed/p6000",
            "vedtakId" to vedtaksId
        )

    fun hentP8000data(sakId: String): P8000AnmodningOmTilleggsinformasjon? =
        get("/sed/p8000",
            "sakId" to sakId
        )

    fun hentP15000data(vedtaksId: String?, sakId: String): P15000overfoeringAvPensjonssakerTilEessiDto? =
        get("/sed/p15000",
            "vedtakId" to vedtaksId,
            "sakId" to sakId
        )


    private inline fun <reified T : Any> get(
        path: String,
        vararg params: Pair<String, String?>
    ): T? {
        val uri = buildUri(path, params)
        logger.debug("Henter pesys informasjon fra: $uri")
        return pesysClientRestTemplate.getForEntity(uri, T::class.java).body
    }

    fun buildUri(
        path: String,
        params: Array<out Pair<String, String?>>
    ): URI =
        UriComponentsBuilder.fromPath(path)
            .apply {
                params
                    .filter { !it.second.isNullOrBlank() }
                    .forEach { queryParam(it.first, it.second) }
            }
            .build()
            .encode()
            .toUri()
}

