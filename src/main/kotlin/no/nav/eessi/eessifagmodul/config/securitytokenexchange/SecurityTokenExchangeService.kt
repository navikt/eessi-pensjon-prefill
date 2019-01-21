package no.nav.eessi.eessifagmodul.config.securitytokenexchange

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.jodah.expiringmap.ExpiringMap
import no.nav.eessi.eessifagmodul.models.SystembrukerTokenException
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.typeRef
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger(SecurityTokenExchangeService::class.java)

data class SecurityTokenResponse(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("token_type")
        val tokenType: String,
        @JsonProperty("expires_in")
        val expiresIn: Long
)

@Service
class SecurityTokenExchangeService(val securityTokenExchangeBasicAuthRestTemplate: RestTemplate) {

    private val tokenCache = ExpiringMap.builder().variableExpiration().build<String, String>()

    fun getSystemOidcToken(): String {

        val token = tokenCache["token"]
        if (!token.isNullOrEmpty()) {
            logger.debug("Using cached token")
            return checkNotNull(token)
        }

        try {
            val uri = UriComponentsBuilder.fromPath("/")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("scope", "openid")
                    .build().toUriString()

            logger.debug("kobler opp mot systembruker token")
            val responseEntity = securityTokenExchangeBasicAuthRestTemplate.exchange(uri, HttpMethod.GET, null, typeRef<SecurityTokenResponse>())
            logger.info("SecurityTokenResponse ${mapAnyToJson(responseEntity)} ")
            validateResponse(responseEntity)
            val accessToken = responseEntity.body!!.accessToken
            val exp = extractExpirationField(accessToken)
            var expiresInSeconds = Duration.between(LocalDateTime.now(), exp).seconds
            // Make the cache-entry expire 30 seconds before the token is no longer valid, to be sure not to use any invalid tokens
            expiresInSeconds = expiresInSeconds.minus(30)

            tokenCache.put("token", accessToken, expiresInSeconds, TimeUnit.SECONDS)
            logger.debug("Added token to cache, expires in $expiresInSeconds seconds")
            return accessToken
        } catch (ex: Exception) {
            logger.error("Feil ved tildeling av token til Systembruker: ${ex.message}", ex)
            throw SystembrukerTokenException(ex.message!!)
        }
    }

    private fun validateResponse(responseEntity: ResponseEntity<SecurityTokenResponse>) {
        if (responseEntity.statusCode.isError)
            throw RuntimeException("SecurityTokenExchange received http-error ${responseEntity.statusCode}:${responseEntity.statusCodeValue}")

    }

    private fun extractExpirationField(jwtString: String): LocalDateTime {
        val parts = jwtString.split('.')
        val expirationTimestamp = jacksonObjectMapper().readTree(Base64.getDecoder().decode(parts[1])).at("/exp")
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(expirationTimestamp.asLong()), ZoneId.systemDefault())
    }
}