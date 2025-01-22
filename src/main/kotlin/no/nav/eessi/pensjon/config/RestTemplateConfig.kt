package no.nav.eessi.pensjon.config

import com.nimbusds.jwt.JWTClaimsSet
import io.micrometer.core.instrument.MeterRegistry
import no.nav.eessi.pensjon.logging.RequestIdHeaderInterceptor
import no.nav.eessi.pensjon.logging.RequestResponseLoggerInterceptor
import no.nav.eessi.pensjon.metrics.RequestCountInterceptor
import no.nav.eessi.pensjon.shared.retry.IOExceptionRetryInterceptor
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpRequest
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.util.*

@Configuration
@Profile("prod", "test")
class RestTemplateConfig(
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService?,
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(RestTemplateConfig::class.java)

    @Value("\${PENSJONSINFORMASJON_URL}")
    lateinit var pensjonUrl: String

    @Value("\${KRR_URL}")
    lateinit var krrUrl: String

    @Bean
    fun pensjoninformasjonRestTemplate() : RestTemplate {
        return RestTemplateBuilder()
            .rootUri(pensjonUrl)
            .errorHandler(DefaultResponseErrorHandler())
            .additionalInterceptors(
                RequestIdHeaderInterceptor(),
                IOExceptionRetryInterceptor(),
                RequestCountInterceptor(meterRegistry),
                RequestResponseLoggerInterceptor(),
                bearerTokenInterceptor(
                    clientConfigurationProperties.registration["proxy-credentials"]
                        ?: throw RuntimeException("could not find oauth2 client config for ${"proxy-credentials"}"),
                    oAuth2AccessTokenService!!
                )
            )
            .build().apply {
                requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
            }
    }

    @Bean
    fun krrRestTemplate() : RestTemplate {
        return RestTemplateBuilder()
            .rootUri(krrUrl)
            .errorHandler(DefaultResponseErrorHandler())
            .additionalInterceptors(
                RequestIdHeaderInterceptor(),
                IOExceptionRetryInterceptor(),
                RequestCountInterceptor(meterRegistry),
                RequestResponseLoggerInterceptor(),
                bearerTokenInterceptor(
                    clientConfigurationProperties.registration["krr-credentials"]
                        ?: throw RuntimeException("could not find oauth2 client config for ${"krr-credentials"}"),
                    oAuth2AccessTokenService!!
                )
            )
            .build().apply {
                requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
            }
    }

    private fun bearerTokenInterceptor(
        clientProperties: ClientProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
            request.headers.setBearerAuth(response.access_token!!)
            val tokenChunks = response.access_token!!.split(".")
            val tokenBody =  tokenChunks[1]
            logger.debug("subject: " + JWTClaimsSet.parse(Base64.getDecoder().decode(tokenBody).decodeToString()).subject + "AccessToken: /n + $response.accessToken")
            execution.execute(request, body!!)
        }
    }
}

