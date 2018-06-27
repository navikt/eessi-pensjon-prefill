package no.nav.eessi.eessifagmodul.config

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