package no.nav.eessi.eessifagmodul.services.eux

import no.nav.eessi.eessifagmodul.config.OidcAuthorizationHeaderInterceptor
import no.nav.eessi.eessifagmodul.config.RequestResponseLoggerInterceptor
import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate

@Component
class EuxRestTemplate(val oidcRequestContextHolder: OIDCRequestContextHolder) {

    @Value("\${euxbasis.v1.url}")
    lateinit var url: String

    @Bean
    fun euxOidcRestTemplate(templateBuilder: RestTemplateBuilder): RestTemplate {
        return templateBuilder
                .rootUri(url)
                .errorHandler(DefaultResponseErrorHandler())
                .additionalInterceptors(RequestResponseLoggerInterceptor(), OidcAuthorizationHeaderInterceptor(oidcRequestContextHolder))
                .build().apply {
                    requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
                }
    }
}