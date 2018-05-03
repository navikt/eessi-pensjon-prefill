package no.nav.eessi.eessifagmodul.services

import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AktoerIdClient {

    @Autowired
    lateinit var service: AktoerV2

    fun ping() {
        service.ping()
    }

    fun hentAktoerIdForIdent(ident: String): HentAktoerIdForIdentResponse? {
        val request = HentAktoerIdForIdentRequest()
        request.ident = ident
        return service.hentAktoerIdForIdent(request)
    }

    fun hentIdentForAktoerId(aktoerId: String): HentIdentForAktoerIdResponse? {
        val request = HentIdentForAktoerIdRequest()
        request.aktoerId = aktoerId
        return service.hentIdentForAktoerId(request)
    }

    fun hentAktoerIdForIdentListe(identListe: List<String>): HentAktoerIdForIdentListeResponse? {
        val request = HentAktoerIdForIdentListeRequest()
        request.identListe.addAll(identListe)
        return service.hentAktoerIdForIdentListe(request)
    }

    fun hentIdentForAktoerIdListe(aktoerIdListe: List<String>): HentIdentForAktoerIdListeResponse? {
        val request = HentIdentForAktoerIdListeRequest()
        request.aktoerIdListe.addAll(aktoerIdListe)
        return service.hentIdentForAktoerIdListe(request)
    }
}
