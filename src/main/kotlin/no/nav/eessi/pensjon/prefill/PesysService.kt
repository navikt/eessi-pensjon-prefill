package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.prefill.models.pensjon.P15000overfoeringAvPensjonssakerTilEessiDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P8000AnmodningOmTilleggsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity

@Service
class PesysService(
    private val pesysClientRestTemplate: RestTemplate
) {

    private val logger: Logger = LoggerFactory.getLogger(PesysService::class.java)

    fun hentP2000data(vedtaksId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? =
        pesysClientRestTemplate.getForEntity<P2xxxMeldingOmPensjonDto>(
            leggTilParameter("/sed/p2000", listOf(
                "vedtaksId" to vedtaksId,
                "fnr" to fnr,
                "sakId" to sakId
            ))
        ).body

    fun hentP2100data(vedtaksId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? =
        pesysClientRestTemplate.getForEntity<P2xxxMeldingOmPensjonDto>(
            leggTilParameter("/sed/p21000", listOf(
                "vedtaksId" to vedtaksId,
                "fnr" to fnr,
                "sakId" to sakId
            ))
        ).body

    fun hentP2200data(vedtaksId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? =
        pesysClientRestTemplate.getForEntity<P2xxxMeldingOmPensjonDto>(
            leggTilParameter("/sed/p22000", listOf(
                "vedtaksId" to vedtaksId,
                "fnr" to fnr,
                "sakId" to sakId
            ))
        ).body

    fun hentP6000data(vedtaksId: String?): P6000MeldingOmVedtakDto? =
        pesysClientRestTemplate.getForEntity<P6000MeldingOmVedtakDto>(
            leggTilParameter("/sed/p6000", listOf(
                "vedtaksId" to vedtaksId
            ))
        ).body

    fun hentP8000data(sakId: String): P8000AnmodningOmTilleggsinformasjon? =
        pesysClientRestTemplate.getForEntity<P8000AnmodningOmTilleggsinformasjon>(
            leggTilParameter("/sed/p8000", listOf(
                "sakId" to sakId
            ))
        ).body

    fun hentP15000data(vedtaksId: String?, sakId: String): P15000overfoeringAvPensjonssakerTilEessiDto? =
        pesysClientRestTemplate.getForEntity<P15000overfoeringAvPensjonssakerTilEessiDto>(
            leggTilParameter("/sed/p15000", listOf(
                "vedtaksId" to vedtaksId,
                "sakId" to sakId
            ))
        ).body

    private fun leggTilParameter(baseUrl: String, params: List<Pair<String, String?>>): String {
        val filtered = params.filter { !it.second.isNullOrBlank() }
        if (filtered.isEmpty()) return baseUrl
        val query = filtered.joinToString("&") { "${it.first}=${it.second}" }
        return "$baseUrl?$query".also { logger.info("Henter pesys informasjon fra: $baseUrl") }
    }
}