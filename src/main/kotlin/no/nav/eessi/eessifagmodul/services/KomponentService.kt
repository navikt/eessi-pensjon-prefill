package no.nav.eessi.eessifagmodul.services

import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDRequest
import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDResponse
import no.nav.eessi.eessifagmodul.models.PENBrukerData

interface KomponentService {

    fun opprettBucogSEDrequest(data : PENBrukerData) : OpprettBuCogSEDRequest

    fun opprettBuCogSEDresponse(request : OpprettBuCogSEDRequest) : OpprettBuCogSEDResponse?

    fun opprettBuCogSED(requestData : PENBrukerData) : OpprettBuCogSEDResponse?


}