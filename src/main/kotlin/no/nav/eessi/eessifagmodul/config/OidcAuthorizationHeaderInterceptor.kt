package no.nav.eessi.eessifagmodul.config

import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse


class OidcAuthorizationHeaderInterceptor(private val oidcRequestContextHolder: OIDCRequestContextHolder) : ClientHttpRequestInterceptor {
    private val logger = LoggerFactory.getLogger(OidcAuthorizationHeaderInterceptor::class.java)

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        if (request.headers[HttpHeaders.AUTHORIZATION] == null) {
            val oidcToken = oidcRequestContextHolder.oidcValidationContext.getToken("oidc").idToken
            logger.debug("Adding Bearer-token to request: $oidcToken")
            request.headers[HttpHeaders.AUTHORIZATION] = "Bearer $oidcToken"
        }
        return execution.execute(request, body)
    }
}
