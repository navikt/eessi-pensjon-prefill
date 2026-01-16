package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity

@Service
class PesysService(
    private val pesysClientRestTemplate: RestTemplate
) {
    fun hentP6000data(vedtaksId: String): P6000MeldingOmVedtakDto? =
        pesysClientRestTemplate.getForEntity<P6000MeldingOmVedtakDto>("/sed/p6000/$vedtaksId").body
}