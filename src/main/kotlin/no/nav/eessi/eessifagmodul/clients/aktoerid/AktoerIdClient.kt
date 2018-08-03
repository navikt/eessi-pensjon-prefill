package no.nav.eessi.eessifagmodul.clients.aktoerid

import no.nav.eessi.eessifagmodul.config.sts.configureRequestSamlToken
import no.nav.eessi.eessifagmodul.config.sts.configureRequestSamlTokenOnBehalfOfOidc
import no.nav.freg.security.oidc.common.OidcTokenAuthentication
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AktoerIdClient(val service: AktoerV2) {

    fun ping() {
        // UNT->SAML: Bruker servicebruker i kall til STS for å hente SAML
        configureRequestSamlToken(service)
        service.ping()
    }

    @Throws(HentIdentForAktoerIdPersonIkkeFunnet::class)
    fun hentAktoerIdForIdent(ident: String): HentAktoerIdForIdentResponse? {
        val auth = SecurityContextHolder.getContext().authentication as OidcTokenAuthentication
        configureRequestSamlTokenOnBehalfOfOidc(service, auth.idToken)

        val request = HentAktoerIdForIdentRequest()
        request.ident = ident
        return service.hentAktoerIdForIdent(request)
    }

    @Throws(HentIdentForAktoerIdPersonIkkeFunnet::class)
    fun hentIdentForAktoerId(aktoerId: String): HentIdentForAktoerIdResponse? {
        val auth = SecurityContextHolder.getContext().authentication as OidcTokenAuthentication
        configureRequestSamlTokenOnBehalfOfOidc(service, auth.idToken)

        val request = HentIdentForAktoerIdRequest().apply {
            setAktoerId(aktoerId)
        }
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
