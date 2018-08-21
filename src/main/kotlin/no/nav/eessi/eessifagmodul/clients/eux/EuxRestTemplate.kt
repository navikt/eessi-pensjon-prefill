package no.nav.eessi.eessifagmodul.clients.eux

import no.nav.eessi.eessifagmodul.config.RequestResponseLoggerInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate


@Component
class EuxRestTemplate {

    @Value("\${eessibasis.url}")
    lateinit var url: String

    @Bean
    fun createEuxRestTemplate(templateBuilder: RestTemplateBuilder): RestTemplate {
        return templateBuilder
                .rootUri(url)
                .errorHandler(DefaultResponseErrorHandler())
                .additionalInterceptors(RequestResponseLoggerInterceptor())
                .build().apply {
                    requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
                }
    }
}