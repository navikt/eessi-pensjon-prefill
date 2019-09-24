package no.nav.eessi.pensjon.logging

import no.nav.eessi.pensjon.fagmodul.prefill.ApiRequest
import no.nav.eessi.pensjon.security.oidc.OidcAuthorizationHeaderInterceptor
import no.nav.eessi.pensjon.utils.toJson
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.security.spring.oidc.SpringOIDCRequestContextHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AuditLogger(private val oidcRequestContextHolder: OIDCRequestContextHolder) {
    private val logger = LoggerFactory.getLogger("auditLogger")

    private val msgkey = "brukerident {} benyttet tjenesten {}  funksjon {}"
    private val msgkeyApi = "brukerident {} benyttet tjenesten {} med request {}  funksjon {}"

    // Vi trenger denne no arg konstruktøren for å kunne bruke @Spy med mockito
    constructor() : this(SpringOIDCRequestContextHolder())

    fun log(event: String, function: String, apiRequest: ApiRequest) {
        logger.info(msgkeyApi, getSubjectfromToken(), event, Context.from(apiRequest).toJson(), function)
    }
    fun log(event: String, function: String) {
        logger.info(msgkey, getSubjectfromToken(), event, function)
    }
    fun log(msg: String, event: String, function: String) {
        logger.info(msg, getSubjectfromToken(), event,  function)
    }

    private fun getSubjectfromToken() : String {
        return try {
            val context = oidcRequestContextHolder.oidcValidationContext
            val tokenContext = OidcAuthorizationHeaderInterceptor(oidcRequestContextHolder).getTokenContext("isso")
            val issuer = tokenContext.issuer
            context.getClaims(issuer).subject
        } catch (ex: Exception) {
            logger.error("Brukerident ikke funnet")
            "n/a"
        }
    }

    private data class Context(
            val sakId: String,
            val vedtakId: String? = null,
            val aktoerId: String? = null,
            val avdodfnr: String? = null,
            val buc: String? = null,
            val sed: String? = null,
            val euxCaseId: String? = null
    ) {
        companion object {
            fun from(apiRequest: ApiRequest): Context {
                return Context(
                        sakId = apiRequest.sakId,
                        vedtakId = apiRequest.vedtakId,
                        aktoerId = apiRequest.aktoerId,
                        avdodfnr = apiRequest.avdodfnr,
                        buc = apiRequest.buc,
                        sed = apiRequest.sed,
                        euxCaseId = apiRequest.euxCaseId)
            }
        }
    }

}