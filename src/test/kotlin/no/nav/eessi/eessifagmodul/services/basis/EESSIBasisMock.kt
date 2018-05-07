package no.nav.eessi.eessifagmodul.services.basis

import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDRequest
import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class EESSIBasisMock : EESSIBasis {

    private val log: Logger = LoggerFactory.getLogger(EESSIBasis::class.java)

    override fun hentResponseData(request: OpprettBuCogSEDRequest): OpprettBuCogSEDResponse? {
        var uuid = UUID.randomUUID()
        var response = OpprettBuCogSEDResponse(uuid, "RINA-12345678", "Statusen er nå")
        return response

    }

}

