package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus.*
import no.nav.eessi.pensjon.prefill.models.pensjon.P15000overfoeringAvPensjonssakerTilEessiDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P8000AnmodningOmTilleggsinformasjon
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import tools.jackson.databind.ObjectMapper

@Service
class PesysService(
    private val pesysClientRestTemplate: RestTemplate
) {

    private val logger: Logger = LoggerFactory.getLogger(PesysService::class.java)
    private val secureLog = LoggerFactory.getLogger("secureLog")

    fun hentP2000data(fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? {
        val response =  getWithHeaders<List<P2xxxMeldingOmPensjonDto>>(
            "/sed/p2000",
            "fnr" to fnr,
            "sakId" to sakId
        )
        return response?.let { returnerSakMedRiktigStatus(it) }
    }

    fun hentP2100data(fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? {
        val response =  getWithHeaders<List<P2xxxMeldingOmPensjonDto>>(
            "/sed/p2100",
            "fnr" to fnr,
            "sakId" to sakId
        )
        return response?.let { returnerSakMedRiktigStatus(it) }
    }

    fun hentP2200data(fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? {
        val response = getWithHeaders<List<P2xxxMeldingOmPensjonDto>>(
            "/sed/p2200",
            "fnr" to fnr,
            "sakId" to sakId
        )
        return response?.let { returnerSakMedRiktigStatus(it) }
    }

    fun returnerSakMedRiktigStatus(response: List<P2xxxMeldingOmPensjonDto>): P2xxxMeldingOmPensjonDto? {
        logger.info("Saker for bruker: ${response.map { it.sak?.status?.name + ":" + it.sak?.sakType }}")
        val resultat =
            response.firstOrNull { it.sak?.status == LOPENDE } ?:
            response.firstOrNull { it.sak?.status == INNV } ?:
            response.firstOrNull { it.sak?.status == TIL_BEHANDLING } ?:
            response.firstOrNull { it.sak?.status == AVSL } ?:
            response.firstOrNull()
        return resultat
    }

    fun hentP6000data(sakId: String?): P6000MeldingOmVedtakDto? {
        val response = getWithHeaders<List<P6000MeldingOmVedtakDto>>(
            "/sed/p6000",
            "sakId" to sakId,
        ).also { secureLog.info("HentSakListe: $it") }

        return response?.sortedByDescending { it.vedtak.datoFattetVedtak }?.firstOrNull()
    }


    fun hentP8000data(sakId: String): P8000AnmodningOmTilleggsinformasjon? =
        getWithHeaders(
            "/sed/p8000",
            "sakId" to sakId
        )

    fun hentP15000data(sakId: String): P15000overfoeringAvPensjonssakerTilEessiDto? {
        val response = getWithHeaders<Any>(
            "/sed/p15000",
            "sakId" to sakId
        )

        val resp = when (response) {
            is List<*> -> response.mapNotNull {
                when (it) {
                    is P15000overfoeringAvPensjonssakerTilEessiDto -> it
                    is Map<*, *> -> ObjectMapper().convertValue(it, P15000overfoeringAvPensjonssakerTilEessiDto::class.java)
                    else -> null
                }
            }

            else -> emptyList()
        }.also { logger.info("HentSakListe: $it") }
        return resp.sortedByAvdodFamilie().firstOrNull()
    }

    fun List<P15000overfoeringAvPensjonssakerTilEessiDto>.sortedByAvdodFamilie(): List<P15000overfoeringAvPensjonssakerTilEessiDto> =
        sortedWith(
            compareByDescending<P15000overfoeringAvPensjonssakerTilEessiDto> { it.sakType != null  }
                .thenByDescending { it.avdod != null  }
                .thenByDescending { it.avdodMor != null  }
                .thenByDescending { it.avdodFar != null  }
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

        logger.info("Henter pesys informasjon fra: $path")

        val entity = HttpEntity<Void>(httpHeaders)
        return pesysClientRestTemplate.exchange<T>(path, HttpMethod.GET, entity).body
            .also { logger.debug("Svar fra Pesys nytt endepunkt: ${it?.toJson()}, url: $path , headers: ${headers.toJson()}") }
    }
}
