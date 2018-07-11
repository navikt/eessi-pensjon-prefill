package no.nav.eessi.eessifagmodul.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.eessifagmodul.models.BUC
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.createPensjonBucList
import org.hibernate.validator.internal.util.Contracts.assertNotNull
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

fun mapAnyToJson(data: Any): String {
    val json = jacksonObjectMapper()
            //.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(data)
    return json
}

fun mapAnyToJson(data: Any, nonempty: Boolean = false): String {
    if (nonempty) {
        val json = jacksonObjectMapper()
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(data)
        return json
    } else {
        return mapAnyToJson(data)
    }
}

fun validateJson(json: String) : Boolean {
    try {
        val objectMapper = ObjectMapper()
        objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
        objectMapper.readTree(json)
        return true
    } catch (ex: Exception) {
        println(ex.message)
    }
    return false
}

fun createListOfSEDOnBUC(buc: BUC): List<SED> {
    val buclist = createPensjonBucList()
    val sedlist : MutableList<SED> = mutableListOf()
    buclist.forEach {
        if (buc.bucType == it.bucType) {
            it.sed!!.forEach {
                sedlist.add(it)
            }
        }
    }
    return sedlist.toList()
}

fun createListOfSED(): List<SED> {
    val buclist = createPensjonBucList()
    val sedlist : MutableList<SED> = mutableListOf()
    buclist.forEach { it: BUC ->
        it.sed!!.forEach {
            sedlist.add(it)
        }
    }
    return sedlist.toList()
}
