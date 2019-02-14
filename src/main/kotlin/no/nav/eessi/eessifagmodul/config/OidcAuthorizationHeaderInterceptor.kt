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
//            var oidcToken : String?  = null
//            try {
//                oidcToken = oidcRequestContextHolder.oidcValidationContext.getToken("oidc").idToken
//                logger.debug("Adding Bearer-token to request: $oidcToken")
//
//            } catch (ex: Exception) {
//                logger.error("Feilet ved OIDC prøver neste")
//                try {
//                    oidcToken = oidcRequestContextHolder.oidcValidationContext.getToken("pesys").idToken
//                    logger.debug("Adding Bearer-token to request: $oidcToken")
//                } catch (ex2: Exception){
//                    logger.error("Ingen TOKEN funner: ${ex.message}")
//                }
//            }
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
    return if (oidcRequestContextHolder.oidcValidationContext.getToken("oidc") != null) {
        logger.info("Fant token på oidc")
        oidcRequestContextHolder.oidcValidationContext.getToken("oidc")
    } else {
        logger.info("Fant token på pesys")
        oidcRequestContextHolder.oidcValidationContext.getToken("pesys")
    }

//    var oidcToken: TokenContext? = null
//    try {
//        oidcToken = oidcRequestContextHolder.oidcValidationContext.getToken("oidc")
//    } catch (ex: Exception) {
//        logger.error("Feilet ved oidc prøver pesys")
//        try {
//            oidcToken = oidcRequestContextHolder.oidcValidationContext.getToken("pesys")
//        } catch (ex2: Exception){
//            logger.error("Ingen TOKEN funner: ${ex.message}")
//        }
//    }
//    return oidcToken!!
}
