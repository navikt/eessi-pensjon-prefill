package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.eux.model.buc.SakStatus
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus.AVSL
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus.INNV
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus.LOPENDE
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus.TIL_BEHANDLING
import no.nav.eessi.pensjon.prefill.models.pensjon.P15000overfoeringAvPensjonssakerTilEessiDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P8000AnmodningOmTilleggsinformasjon
import no.nav.eessi.pensjon.prefill.sed.krav.PrefillP2xxxPensjon.logger
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.ObjectMapper

@Service
class PesysService(
    private val pesysClientRestTemplate: RestTemplate
) {

    private val logger: Logger = LoggerFactory.getLogger(PesysService::class.java)

    fun hentP2000data(vedtakId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? {
        val response = getWithHeaders<Any>(
            "/sed/p2000",
            "vedtakId" to vedtakId,
            "fnr" to fnr,
            "sakId" to sakId
        )
        return p2xxxFraListe(response)
    }

    fun hentP2100data(vedtakId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? {
        val response = getWithHeaders<Any>(
            "/sed/p2100",
            "vedtakId" to vedtakId,
            "fnr" to fnr,
            "sakId" to sakId
        )
        return p2xxxFraListe(response)
    }

    fun hentP2200data(vedtakId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? {
        val response = getWithHeaders<Any>(
            "/sed/p2200",
            "vedtakId" to vedtakId,
            "fnr" to fnr,
            "sakId" to sakId
        )
        return p2xxxFraListe(response)
    }

     fun p2xxxFraListe(response: Any?): P2xxxMeldingOmPensjonDto? {
        logger.debug("p2xxxFraListe: {}", response)

        val resp = when (response) {
            is List<*> -> response.flatMap { mapToP2xxxList(it) }
            else -> mapToP2xxxList(response)
        }.also { logger.info("HentSakListe: $it") }

        return returnerSakMedRiktigStatus(resp)
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


    private fun mapToP2xxxList(value: Any?): List<P2xxxMeldingOmPensjonDto> {
        return when (value) {
            null -> emptyList()
            is P2xxxMeldingOmPensjonDto -> listOf(value)
            is Map<*, *> -> listOf(ObjectMapper().convertValue(value, P2xxxMeldingOmPensjonDto::class.java))
            else -> parseP2xxxJson(value.toString())
        }
    }

    private fun parseP2xxxJson(rawJson: String): List<P2xxxMeldingOmPensjonDto> {
        val json = rawJson.trim()
        if (json.isEmpty()) return emptyList()

        return if (json.startsWith("[")) {
            mapJsonToAny<List<P2xxxMeldingOmPensjonDto>>(json)
        } else {
            listOf(mapJsonToAny<P2xxxMeldingOmPensjonDto>(json))
        }
    }

    fun hentP6000data(sakId: String?): P6000MeldingOmVedtakDto? {
        val response = getWithHeaders<Any>(
            "/sed/p6000",
            "sakId" to sakId,
        )

        val resp = when (response) {
            is List<*> -> response.mapNotNull {
                when (it) {
                    is P6000MeldingOmVedtakDto -> it
                    is Map<*, *> -> ObjectMapper().convertValue(it, P6000MeldingOmVedtakDto::class.java)
                    else -> null
                }
            }

            else -> emptyList()
        }.also { logger.info("HentSakListe: $it") }
        return resp.sortedByDescending { it.vedtak.datoFattetVedtak }.firstOrNull()
    }


    fun hentP8000data(sakId: String): P8000AnmodningOmTilleggsinformasjon? =
        getWithHeaders(
            "/sed/p8000",
            "sakId" to sakId
        )

    fun hentP15000data(vedtakId: String?, sakId: String): P15000overfoeringAvPensjonssakerTilEessiDto? {
        val response = getWithHeaders<Any>(
            "/sed/p15000",
            "vedtakId" to vedtakId,
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
        return resp.firstOrNull()
    }


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
        return pesysClientRestTemplate.exchange(path, HttpMethod.GET, entity, T::class.java).body
            .also { logger.debug("Svar fra Pesys nytt endepunkt: ${it?.toJson()}, url: $path , headers: ${headers.toJson()}") }
    }
}
