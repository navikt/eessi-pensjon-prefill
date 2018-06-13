package no.nav.eessi.eessifagmodul.security.jaxws.client

import no.nav.eessi.eessifagmodul.security.jaxws.sts.configureRequestSamlToken
import no.nav.eessi.eessifagmodul.security.jaxws.sts.configureRequestSamlTokenOnBehalfOfOidc
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AktoerIdClient {

    @Autowired
    lateinit var service: AktoerV2

    fun ping() {
        // UNT->SAML: Bruker servicebruker i kall til STS for å hente SAML
        configureRequestSamlToken(service)
        service.ping()
    }

    fun hentAktoerIdForIdent(ident: String, oidcToken: String): HentAktoerIdForIdentResponse? {
        // OIDC->SAML: Bruker oidctoken fra konsument sin request i kall til STS for å hente SAML
        configureRequestSamlTokenOnBehalfOfOidc(service, oidcToken)

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
