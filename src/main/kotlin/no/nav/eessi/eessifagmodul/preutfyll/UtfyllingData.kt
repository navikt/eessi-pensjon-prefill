package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.FrontendRequest
import no.nav.eessi.eessifagmodul.models.SED

class UtfyllingData(val sed: SED, val request: FrontendRequest, val pin: String? = "") {

    val beskrivelse: String = ""

    //Pinid (FNR) aktorID
    fun hentPinid(): String? { return pin }
    fun hentAktorid(): String { return request.pinid!! }

}

