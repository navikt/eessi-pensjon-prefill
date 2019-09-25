package no.nav.eessi.pensjon.logging

import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.security.oidc.context.TokenContext
import no.nav.security.spring.oidc.SpringOIDCRequestContextHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AuditLogger(private val oidcRequestContextHolder: OIDCRequestContextHolder) {
    private val logger = LoggerFactory.getLogger("auditLogger")

    private val msgkey = "brukerident {} benyttet tjenesten {}  funksjon {}"
    private val msgkeyApi = "brukerident {} benyttet tjenesten {} med request {}  funksjon {}"
    private val msgKeyErr = "brukerident {} benyttet tjenesten {} medfører feil av type {}"

    // Vi trenger denne no arg konstruktøren for å kunne bruke @Spy med mockito
    constructor() : this(SpringOIDCRequestContextHolder())

    fun log(event: String, function: String, context: String) {
        logger.info(msgkeyApi, getSubjectfromToken(), event, context, function)
    }
    fun log(event: String, function: String) {
        logger.info(msgkey, getSubjectfromToken(), event, function)
    }
    fun logErr(msg: String, event: String, function: String) {
        logger.info(msg, getSubjectfromToken(), event,  function)
    }

    private fun getSubjectfromToken() : String {
        return try {
            val context = oidcRequestContextHolder.oidcValidationContext
            val tokenContext = getTokenContext("isso")
            val issuer = tokenContext.issuer
            context.getClaims(issuer).subject
        } catch (ex: Exception) {
            logger.error("Brukerident ikke funnet")
            "n/a"
        }
    }

    private fun getTokenContext(tokenKey: String): TokenContext {
        val context = oidcRequestContextHolder.oidcValidationContext
        if (context.issuers.isEmpty()) throw RuntimeException("No issuer found in context")
        val tokenkeys = context.issuers
        if (tokenkeys.contains(tokenKey)) {
            return context.getToken(tokenKey)
        }
        throw RuntimeException("No issuer found in context")
    }



}