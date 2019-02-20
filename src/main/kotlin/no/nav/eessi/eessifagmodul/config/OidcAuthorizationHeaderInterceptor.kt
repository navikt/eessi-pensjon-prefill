package no.nav.eessi.eessifagmodul.config

import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.security.oidc.context.TokenContext
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse


private val logger = LoggerFactory.getLogger(OidcAuthorizationHeaderInterceptor::class.java)

class OidcAuthorizationHeaderInterceptor(private val oidcRequestContextHolder: OIDCRequestContextHolder) : ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        logger.info("sjekker reqiest header for AUTH")
        if (request.headers[HttpHeaders.AUTHORIZATION] == null) {
            val oidcToken = getIdTokenFromIssuer(oidcRequestContextHolder)
            logger.info("Adding Bearer-token to request: $oidcToken")
            request.headers[HttpHeaders.AUTHORIZATION] = "Bearer $oidcToken"
        }
        return execution.execute(request, body)
    }
}

fun getIdTokenFromIssuer(oidcRequestContextHolder: OIDCRequestContextHolder): String {
    return getTokenContextFromIssuer(oidcRequestContextHolder).idToken
}

fun getTokenContextFromIssuer(oidcRequestContextHolder: OIDCRequestContextHolder): TokenContext {
    //oidcRequestContextHolder.oidcValidationContext.getToken("oidc")
    //return context.getToken(issuer)
    val context = oidcRequestContextHolder.oidcValidationContext
    if (context.issuers.isEmpty()) {
        throw RuntimeException("No issuer found in context")
    }
    logger.info("Returning token on : oidc")
    return context.getToken("oidc")
}
