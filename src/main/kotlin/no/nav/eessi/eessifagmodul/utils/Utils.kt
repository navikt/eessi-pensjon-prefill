package no.nav.eessi.eessifagmodul.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClientException

class Utils

val logger: Logger by lazy { LoggerFactory.getLogger(Utils::class.java) }


inline fun <reified T : Any> typeRef(): ParameterizedTypeReference<T> = object : ParameterizedTypeReference<T>() {}
inline fun <reified T : Any> typeRefs(): TypeReference<T> = object : TypeReference<T>() {}

/**
 * Sende httpheader til BASSIS token/cookie?
 */
fun createHeaderData(token: String): HttpHeaders {
    val headers = HttpHeaders()
    headers.add(HttpHeaders.COOKIE, "JSESSIONID=$token")
    headers.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8")
    return headers
}

fun createErrorMessage(responseBody: String?): RestClientException {
    val objectMapper = jacksonObjectMapper()
    logger.error("ErrorMessage (responseBody) : $responseBody")
    return objectMapper.readValue(responseBody, RestClientException::class.java)
}