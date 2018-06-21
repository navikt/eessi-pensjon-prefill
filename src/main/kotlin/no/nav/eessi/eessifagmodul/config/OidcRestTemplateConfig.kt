package no.nav.eessi.eessifagmodul.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate


@Configuration
class OidcRestTemplateConfig {

    @Value("\${eessibasis.url}")
    lateinit var url: String

    @Bean
    fun createOidcRestTemplate(templateBuilder: RestTemplateBuilder): RestTemplate {
        val resttemplate: RestTemplate = templateBuilder
                .rootUri(url)
                .errorHandler(DefaultResponseErrorHandler())
                //.errorHandler(EESSIResponeErrorHandler())
                .additionalInterceptors(RequestResponseLoggerInterceptor())
                .build()
        val factory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
        resttemplate.requestFactory = factory
        return resttemplate
    }
}