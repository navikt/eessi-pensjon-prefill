package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.prefill.models.pensjon.P15000overfoeringAvPensjonssakerTilEessiDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P8000AnmodningOmTilleggsinformasjon
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity

@Service
class PesysService(
    private val pesysClientRestTemplate: RestTemplate
) {

    fun hentP2000data(vedtaksId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? =
        pesysClientRestTemplate.getForEntity<P2xxxMeldingOmPensjonDto>("/sed/p2000/$vedtaksId").body

    fun hentP2100data(vedtaksId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? =
        pesysClientRestTemplate.getForEntity<P2xxxMeldingOmPensjonDto>("/sed/p21000/$vedtaksId").body

    fun hentP2200data(vedtaksId: String?, fnr: String, sakId: String): P2xxxMeldingOmPensjonDto? =
        pesysClientRestTemplate.getForEntity<P2xxxMeldingOmPensjonDto>("/sed/p22000/$vedtaksId").body

    fun hentP6000data(vedtaksId: String?): P6000MeldingOmVedtakDto? =
        pesysClientRestTemplate.getForEntity<P6000MeldingOmVedtakDto>("/sed/p6000/$vedtaksId").body

    fun hentP8000data(sakId: String): P8000AnmodningOmTilleggsinformasjon? =
        pesysClientRestTemplate.getForEntity<P8000AnmodningOmTilleggsinformasjon>("/sed/p8000/$sakId").body

    fun hentP15000data(vedtaksId: String?, sakId: String): P15000overfoeringAvPensjonssakerTilEessiDto? =
        pesysClientRestTemplate.getForEntity<P15000overfoeringAvPensjonssakerTilEessiDto>("/sed/p15000/$vedtaksId").body

}