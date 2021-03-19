package no.nav.eessi.pensjon.fagmodul.eux

import io.micrometer.core.instrument.MeterRegistry
import no.nav.eessi.pensjon.logging.RequestIdHeaderInterceptor
import no.nav.eessi.pensjon.logging.RequestResponseLoggerInterceptor
import no.nav.eessi.pensjon.metrics.RequestCountInterceptor
import no.nav.eessi.pensjon.security.sts.STSService
import no.nav.eessi.pensjon.security.sts.UsernameToOidcInterceptor
import no.nav.eessi.pensjon.security.token.TokenAuthorizationHeaderInterceptor
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Component
class EuxRestTemplate(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val registry: MeterRegistry,
    private val stsService: STSService
) {

    @Value("\${eessipen-eux-rina.url}")
    lateinit var url: String

    @Value("\${NAIS_NAMESPACE}")
    private lateinit var nameSpace: String

    private val logger = LoggerFactory.getLogger(EuxRestTemplate::class.java)

    @Bean
    fun euxOidcRestTemplate(templateBuilder: RestTemplateBuilder): RestTemplate {
        return templateBuilder
                .rootUri(url)
                .errorHandler(DefaultResponseErrorHandler())
                .setReadTimeout(Duration.ofSeconds(120))
                .setConnectTimeout(Duration.ofSeconds(120))
                .additionalInterceptors(
                        RequestIdHeaderInterceptor(),
                        RequestCountInterceptor(registry),
                        RequestResponseLoggerInterceptor(),
                        getTokenInterceptor(nameSpace))
                .build().apply {
                    requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
                }
    }

    fun getTokenInterceptor(namespace: String): ClientHttpRequestInterceptor {
        return if (nameSpace == "q2" || nameSpace == "test") {
            logger.debug("UsernameToOidcInterceptor - systembruker")
            UsernameToOidcInterceptor(stsService)
        } else {
            logger.debug("TokenAuthorizationInterceptor - bruker")
            TokenAuthorizationHeaderInterceptor(tokenValidationContextHolder)
        }
    }

}
