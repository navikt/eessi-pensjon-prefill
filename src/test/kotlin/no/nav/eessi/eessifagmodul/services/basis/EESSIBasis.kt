package no.nav.eessi.eessifagmodul.services.basis

import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDRequest
import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDResponse


interface EESSIBasis : BasisRequestResponse<OpprettBuCogSEDResponse, OpprettBuCogSEDRequest> {

    override fun hentResponseData(request: OpprettBuCogSEDRequest): OpprettBuCogSEDResponse?

}