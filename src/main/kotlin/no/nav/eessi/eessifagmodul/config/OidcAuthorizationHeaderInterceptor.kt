package no.nav.eessi.eessifagmodul.config

import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.security.oidc.context.OIDCValidationContext
import no.nav.security.oidc.context.TokenContext
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.util.*


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

    fun getIdTokenFromIssuer(oidcRequestContextHolder: OIDCRequestContextHolder): String {
        return getTokenContextFromIssuer(oidcRequestContextHolder).idToken
    }

    fun getTokenContextFromIssuer(oidcRequestContextHolder: OIDCRequestContextHolder): TokenContext {
        val context = oidcRequestContextHolder.oidcValidationContext
        if (context.issuers.isEmpty()) throw RuntimeException("No issuer found in context")
        // At this point more than one issuer is not supporteted. May be changed later.
        if (context.issuers.size > 1) throw RuntimeException("More than one issuer found in context. ")

        logger.debug("Returning token on : ${context.issuers.first()}")
        return context.getToken(context.issuers.first())
    }

}

//Magic find token that has longest time or just exist in context.issuer list
//compare expiretime with other token of more than one is found.
class OidcAuthorizationHeaderInterceptorMagic(private val oidcRequestContextHolder: OIDCRequestContextHolder) : ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        logger.info("sjekker reqiest header for AUTH")
        if (request.headers[HttpHeaders.AUTHORIZATION] == null) {
            val oidcToken = getIdTokenFromIssuer(oidcRequestContextHolder)
            logger.info("Adding Bearer-token to request: $oidcToken")
            request.headers[HttpHeaders.AUTHORIZATION] = "Bearer $oidcToken"
        }
        return execution.execute(request, body)
    }

    fun getIdTokenFromIssuer(oidcRequestContextHolder: OIDCRequestContextHolder): String {
        return getTokenContextFromIssuer(oidcRequestContextHolder).idToken
    }

    fun getTokenContextFromIssuer(oidcRequestContextHolder: OIDCRequestContextHolder): TokenContext {
        val context = oidcRequestContextHolder.oidcValidationContext
        if (context.issuers.isEmpty()) throw RuntimeException("No issuer found in context")

        //supportet token-support token-keys.:
        //var tokenkeys = listOf("isso","oidc","pesys")
        val tokenkeys = context.issuers
        logger.debug("Found : ${tokenkeys.size} valid issuers")

        var found = mutableListOf<TokenContext>()
        var token: TokenContext ?= null
        tokenkeys.forEach {
            token = context.getToken(it)
            if (token != null) {
                logger.debug("Found token on issuer: $it with token: ${token}")
                found.add(token!!)
            }
        }
        if (found.size == 1) {
            val tokenContext = found[0]
            logger.debug("Only one ISSUER found. Returning first! issuer-key: ${tokenContext.issuer}")
            return tokenContext
        }

        logger.debug("More than one ISSUER found.: ${found.size}")
        //val foundToken = mutableListOf<>()
        val foundToken = found.asSequence().sortedBy { it.issuer }.toList()

        var longest = foundToken[0]
        var longestExpirationTime = getExpirationTime(context, longest.issuer)

        for(tokenX  in foundToken) {
            val nextTime = getExpirationTime(context, tokenX.issuer)

            logger.debug("Compare longestTime: $longestExpirationTime with nextTime: $nextTime")
            if (longestExpirationTime.after(nextTime)) {
                logger.debug("Do nothing after?")
            } else {
                logger.debug("longestTime before nextTime. nextTime new longestTime")
                longest = tokenX
                longestExpirationTime = nextTime
            }

        }
        logger.debug("Returing following issuer: ${longest.issuer}, exp: ${longestExpirationTime},\ntoken: ${longest.idToken}")
        return longest
    }

    private fun getExpirationTime(context: OIDCValidationContext, issuer: String): Date {

        context.firstValidToken
        val jwtset =  context.getClaims(issuer).claimSet
        val expirationTime = jwtset.expirationTime
        logger.debug("Found issuer: ${issuer} expirationTime: ${expirationTime}")
        return expirationTime
    }

}


class OidcAuthorizationHeaderInterceptorSelectIssuer(private val oidcRequestContextHolder: OIDCRequestContextHolder, private val issuer: String) : ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        logger.info("sjekker reqiest header for AUTH")
        if (request.headers[HttpHeaders.AUTHORIZATION] == null) {
            val oidcToken = getIdTokenFromSelectedIssuer(oidcRequestContextHolder, issuer)
            logger.info("Adding Bearer-token to request: $oidcToken")
            request.headers[HttpHeaders.AUTHORIZATION] = "Bearer $oidcToken"
        }
        return execution.execute(request, body)
    }

    fun getIdTokenFromSelectedIssuer(oidcRequestContextHolder: OIDCRequestContextHolder, issuer: String): String {
        return getTokenContextFromSelectedIssuer(oidcRequestContextHolder, issuer).idToken
    }

    fun getTokenContextFromSelectedIssuer(oidcRequestContextHolder: OIDCRequestContextHolder, issuer: String): TokenContext {
        val context = oidcRequestContextHolder.oidcValidationContext
        if (context.issuers.isEmpty()) throw RuntimeException("No issuer found in context")
        // At this point more than one, select one to use.
        logger.debug("Returning token on issuer: $issuer with token: ${context.getToken(issuer)}")
        return context.getToken(issuer)

    }
}

class OidcAuthorizationHeaderInterceptorSetIssuer(private val oidcRequestContextHolder: OIDCRequestContextHolder, private val issuer: Set<String>) : ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        logger.info("sjekker reqiest header for AUTH")
        if (request.headers[HttpHeaders.AUTHORIZATION] == null) {
            val oidcToken = getIdTokenFromSelectedIssuer(oidcRequestContextHolder, issuer)
            logger.info("Adding Bearer-token to request: $oidcToken")
            request.headers[HttpHeaders.AUTHORIZATION] = "Bearer $oidcToken"
        }
        return execution.execute(request, body)
    }

    fun getIdTokenFromSelectedIssuer(oidcRequestContextHolder: OIDCRequestContextHolder, issuer: Set<String>): String {
        return getTokenContextFromSelectedIssuer(oidcRequestContextHolder, issuer).idToken
    }

    fun getTokenContextFromSelectedIssuer(oidcRequestContextHolder: OIDCRequestContextHolder, issuer: Set<String>): TokenContext {
        val context = oidcRequestContextHolder.oidcValidationContext
        if (context.issuers.isEmpty()) throw RuntimeException("No issuer found in context")
        // At this point more than one, select one to use.
        var token: TokenContext ?= null
        issuer.forEach {
            token = context.getToken(it)
            if (token != null) {
                logger.debug("Returning token on issuer: $issuer with token: ${token}")
                return token!!
            }
        }
        logger.debug("Returning blank token on issuer: $issuer with token: null")
        return token!!
   }
}




