package no.nav.eessi.eessifagmodul.services.pensjonsinformasjon

import no.nav.eessi.eessifagmodul.config.OidcAuthorizationHeaderInterceptor
import no.nav.eessi.eessifagmodul.config.RequestResponseLoggerInterceptor
import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class PensjonsinformasjonRestTemplate(val oidcRequestContextHolder: OIDCRequestContextHolder) {
    // TODO: Fjern denne defaulten etter pensjon-fss eksponerer tjenesten i Fasit
    @Value("\${Pensjonsinformasjon.url:https://wasapp-t5.adeo.no/pensjon-ws/api/pensjonsinformasjon/v1}")
    lateinit var url: String

    @Bean
    fun pensjonsinformasjonOidcRestTemplate(templateBuilder: RestTemplateBuilder): RestTemplate {
        return templateBuilder
                .rootUri(url)
                .additionalInterceptors(RequestResponseLoggerInterceptor(), OidcAuthorizationHeaderInterceptor(oidcRequestContextHolder))
                .build().apply {
                    requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
                }
    }
}