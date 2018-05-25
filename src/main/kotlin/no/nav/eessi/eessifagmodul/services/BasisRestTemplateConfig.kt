package no.nav.eessi.eessifagmodul.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.support.BasicAuthorizationInterceptor
import org.springframework.web.client.RestTemplate

@Configuration
class BasisRestTemplateConfig {

    private val logger: Logger = LoggerFactory.getLogger(BasisRestTemplateConfig::class.java)

    @Value("\${eessibasis.url}")
    lateinit var url: String

    @Value("\${eessibasis.username}")
    lateinit var userName: String

    @Value("\${eessibasis.password}")
    lateinit var passWord: String

    @Bean
    fun byggTemplate(templateBuilder: RestTemplateBuilder): RestTemplate {
        logger.debug("========================================\n")
        logger.debug("BasisRestTemplateConfig - byggTemplate URL : $url  ")
        logger.debug("")
        logger.debug("========================================\n")
        return templateBuilder.rootUri(url).build()
    }

    @Bean
    fun byggEESSI(templateBuilder: RestTemplateBuilder): EESSIRest {
        logger.debug("========================================\n")
        logger.debug("BasisRestTemplateConfig - byggEESSI (EESSIRest) URL : $url  ")
        logger.debug("")
        logger.debug("========================================\n")
        val rest = EESSIRest()
        rest.url = url
        rest.build = templateBuilder
        rest.restTemplate = templateBuilder.rootUri(url).build()
        rest.authorization = BasicAuthorizationInterceptor(userName, passWord)
        logger.debug("toString " + rest.toString())
        return rest
    }

}