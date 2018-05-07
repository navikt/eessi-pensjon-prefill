package no.nav.eessi.eessifagmodul.services.basis

import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDRequest
import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.RestTemplate

class EESSIBasisImp : EESSIBasis {

    private val log: Logger = LoggerFactory.getLogger(EESSIBasis::class.java)

    @Autowired
    lateinit var restTemplate: RestTemplate

    override fun hentResponseData(request: OpprettBuCogSEDRequest): OpprettBuCogSEDResponse? {
        log.debug("EESSIBasisImp : " + restTemplate.uriTemplateHandler.toString())
        val response = restTemplate.postForObject("/", request, OpprettBuCogSEDResponse::class.java)
        return response
    }


}