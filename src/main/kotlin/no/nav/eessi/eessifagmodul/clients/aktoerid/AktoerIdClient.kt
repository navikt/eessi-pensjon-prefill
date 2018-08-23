package no.nav.eessi.eessifagmodul.clients.aktoerid

import no.nav.eessi.eessifagmodul.config.sts.configureRequestSamlToken
import no.nav.eessi.eessifagmodul.config.sts.configureRequestSamlTokenOnBehalfOfOidc
import no.nav.eessi.eessifagmodul.models.PersonIkkeFunnetException
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AktoerIdClient(val service: AktoerV2, val oidcRequestContextHolder: OIDCRequestContextHolder) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(AktoerIdClient::class.java) }

    fun ping() {
        // UNT->SAML: Bruker servicebruker i kall til STS for å hente SAML
        configureRequestSamlToken(service)
        service.ping()
    }

    fun hentAktoerIdForIdent(ident: String): HentAktoerIdForIdentResponse? {
        val token = oidcRequestContextHolder.oidcValidationContext.getToken("oidc")
        configureRequestSamlTokenOnBehalfOfOidc(service, token.idToken)

        val request = HentAktoerIdForIdentRequest()
        request.ident = ident
        return service.hentAktoerIdForIdent(request)
    }

    fun hentIdentForAktoerId(aktoerId: String): HentIdentForAktoerIdResponse {
        val token = oidcRequestContextHolder.oidcValidationContext.getToken("oidc")
        configureRequestSamlTokenOnBehalfOfOidc(service, token.idToken)

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

    @Throws(PersonIkkeFunnetException::class)
    fun hentPinIdentFraAktorid(pin: String = ""): String {
        return try {
            hentIdentForAktoerId(aktoerId = pin).ident
        } catch (err: HentIdentForAktoerIdPersonIkkeFunnet) {
            logger.error(err.message)
            throw PersonIkkeFunnetException("Fant ikke aktoer", err)
        } catch (ex: Exception) {
            logger.error(ex.message)
            throw Exception(ex.message, ex)
        }
    }
}
