package no.nav.eessi.pensjon.services.kodeverk

import io.micrometer.core.instrument.MeterRegistry
import no.nav.eessi.pensjon.logging.RequestIdHeaderInterceptor
import no.nav.eessi.pensjon.logging.RequestResponseLoggerInterceptor
import no.nav.eessi.pensjon.metrics.RequestCountInterceptor
import no.nav.eessi.pensjon.security.oidc.OidcAuthorizationHeaderInterceptor
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
class KodeverkRestTemplate(private val oidcRequestContextHolder: OIDCRequestContextHolder,
                           private val registry: MeterRegistry) {

    @Value("\${kodeverk.rest-api.url}")
    private lateinit var kodeverkUrl: String

    @Bean
    fun kodeverkOidcRestTemplate(templateBuilder: RestTemplateBuilder): RestTemplate {
        return templateBuilder
                .rootUri(kodeverkUrl)
                .errorHandler(DefaultResponseErrorHandler())
                .additionalInterceptors(
                        RequestIdHeaderInterceptor(),
                        RequestCountInterceptor(registry),
                        RequestResponseLoggerInterceptor(),
                        OidcAuthorizationHeaderInterceptor(oidcRequestContextHolder))
                .build().apply {
                    requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
                }
    }

}

