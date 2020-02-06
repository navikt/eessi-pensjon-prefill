package no.nav.eessi.pensjon.security.oidc

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


//Find token that has longest time or just exist in context.issuer list
//compare expiretime with other token of more than one is found.
class OidcAuthorizationHeaderInterceptor(private val oidcRequestContextHolder: OIDCRequestContextHolder) : ClientHttpRequestInterceptor {

    private val logger = LoggerFactory.getLogger(OidcAuthorizationHeaderInterceptor::class.java)

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        logger.info("sjekker request header for AUTH")

        if (request.headers[HttpHeaders.AUTHORIZATION] == null) {
            val oidcToken = getIdTokenFromIssuer(oidcRequestContextHolder)
            request.headers[HttpHeaders.AUTHORIZATION] = "Bearer $oidcToken"
            logger.debug("setter HttpHeaders.AUTHORIZATION med Bearer token: $oidcToken")
        } else {
            logger.debug("HttpHeaders.AUTHORIZATION alt med token: ${request.headers[HttpHeaders.AUTHORIZATION]}")
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
        logger.info("Found : ${tokenkeys.size} valid issuers")

        val foundListOfIssuers = tokenkeys.filter { key -> context.getToken(key) != null }
                .map { key -> context.getToken(key) }
                .sortedBy { key -> key.issuer }
                .toList()

        if (foundListOfIssuers.size == 1) {
            val tokenContext = foundListOfIssuers.first()
            logger.info("Only one ISSUER found. Returning first! issuer-key: ${tokenContext.issuer}")
            getExpirationTime(context, tokenContext.issuer)
            return tokenContext
        }

        logger.info("More than one ISSUER found. Number of issuers found: ${foundListOfIssuers.size}")

        //hente ut første token med utløpstid
        var longestLivingToken = foundListOfIssuers.first()
        var previousTokenExpirationTime = getExpirationTime(context, longestLivingToken.issuer)

        //iterere igjennom alle token for å finne den med lengst utløpstid
        for(selectedToken in foundListOfIssuers) {
            val currentTokenExpirationTime = getExpirationTime(context, selectedToken.issuer)

            logger.debug("Compare longestTime: $previousTokenExpirationTime with nextExpirationTime: $currentTokenExpirationTime")
            if (previousTokenExpirationTime.after(currentTokenExpirationTime)) {
                logger.debug("Do nothing after?")
            } else {
                logger.debug("longestTime before nextTime. nextTime new longestTime")
                longestLivingToken = selectedToken
                previousTokenExpirationTime = currentTokenExpirationTime
            }

        }
        logger.info("Returning following issuer: ${longestLivingToken.issuer}, exp: $previousTokenExpirationTime")
        return longestLivingToken
    }

    private fun getExpirationTime(context: OIDCValidationContext, issuer: String): Date {
        context.firstValidToken
        val jwtset =  context.getClaims(issuer).claimSet
        val expirationTime = jwtset.expirationTime
        logger.info("Found issuer: $issuer with extra data:")
        logger.info("ExpirationTime: $expirationTime")
        logger.info("CreateTime: ${jwtset.issueTime}")
        if (issuer == "servicebruker") {
            logger.info("Subject: ${jwtset.subject}")
        } else {
            logger.debug("Subject: ${jwtset.subject}")
        }
        return expirationTime
    }

}

