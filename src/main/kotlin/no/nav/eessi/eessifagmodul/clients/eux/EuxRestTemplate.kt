package no.nav.eessi.eessifagmodul.clients.eux

import no.nav.eessi.eessifagmodul.config.RequestResponseLoggerInterceptor
import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.*
import org.springframework.stereotype.Component
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate

private val logger: Logger by lazy { LoggerFactory.getLogger(EuxRestTemplate::class.java) }

@Component
class EuxRestTemplate(val oidcRequestContextHolder: OIDCRequestContextHolder) {

    @Value("\${eessibasis.url}")
    lateinit var url: String

    @Bean
    fun createEuxRestTemplate(templateBuilder: RestTemplateBuilder): RestTemplate {
        return templateBuilder
                .rootUri(url)
                .errorHandler(DefaultResponseErrorHandler())
                .additionalInterceptors(RequestResponseLoggerInterceptor(), OidcAuthorizationHeaderInterceptor(oidcRequestContextHolder))
                .build().apply {
                    requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
                }
    }

    class OidcAuthorizationHeaderInterceptor(private val oidcRequestContextHolder: OIDCRequestContextHolder) : ClientHttpRequestInterceptor {
        override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
            request.headers[HttpHeaders.AUTHORIZATION]?.let {
                val oidcToken = oidcRequestContextHolder.oidcValidationContext.getToken("oidc").idToken
                logger.debug("Adding Bearer-token to request: $oidcToken")
                request.headers[HttpHeaders.AUTHORIZATION] = "Bearer $oidcToken"
            }
            return execution.execute(request, body)
        }
    }
}