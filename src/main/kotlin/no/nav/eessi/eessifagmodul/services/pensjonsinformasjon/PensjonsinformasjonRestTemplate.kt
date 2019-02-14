package no.nav.eessi.eessifagmodul.services.pensjonsinformasjon

import io.micrometer.core.instrument.MeterRegistry
import no.nav.eessi.eessifagmodul.config.RequestResponseLoggerInterceptor
import no.nav.eessi.eessifagmodul.config.securitytokenexchange.SecurityTokenExchangeService
import no.nav.eessi.eessifagmodul.config.securitytokenexchange.UntToOidcInterceptor
import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.metrics.web.client.DefaultRestTemplateExchangeTagsProvider
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class PensjonsinformasjonRestTemplate(val securityTokenExchangeService: SecurityTokenExchangeService, val oidcRequestContextHolder: OIDCRequestContextHolder, private val registry: MeterRegistry) {
    //(val oidcRequestContextHolder: OIDCRequestContextHolder) {
    // TODO: Fjern denne defaulten etter pensjon-fss eksponerer tjenesten i Fasit
    //@Value("\${pensjonsinformasjon.api.v1.url:https://wasapp-t4.adeo.no/pensjon-ws/api/pensjonsinformasjon}")
    //:https://wasapp-t5.adeo.no/pensjon-ws/api/pensjonsinformasjon/v1
    @Value("\${pensjonsinformasjon.url}")
    lateinit var url: String

    @Bean
    fun pensjonsinformasjonOidcRestTemplate(templateBuilder: RestTemplateBuilder): RestTemplate {
        return templateBuilder
                .rootUri(url)
                .additionalInterceptors(RequestResponseLoggerInterceptor(), UntToOidcInterceptor(securityTokenExchangeService))
                //.additionalInterceptors(RequestResponseLoggerInterceptor(), OidcAuthorizationHeaderInterceptor(oidcRequestContextHolder))
                .customizers(MetricsRestTemplateCustomizer(registry, DefaultRestTemplateExchangeTagsProvider(), "eessipensjon_fagmodul_pensjonsinformasjon"))
                .build().apply {
                    requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
                }
    }
}