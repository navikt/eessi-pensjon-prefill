package no.nav.eessi.eessifagmodul.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.eessifagmodul.models.BUC
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.createPensjonBucList
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClientException


//class Utils

inline fun <reified T : Any> typeRef(): ParameterizedTypeReference<T> = object : ParameterizedTypeReference<T>() {}
inline fun <reified T : Any> typeRefs(): TypeReference<T> = object : TypeReference<T>() {}

fun createErrorMessage(responseBody: String?): RestClientException {
    val objectMapper = jacksonObjectMapper()
    return objectMapper.readValue(responseBody, RestClientException::class.java)
}

fun mapAnyToJson(data: Any): String {
    val json = jacksonObjectMapper()
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

inline fun <reified T : Any> mapJsonToAny(json: String, objec : TypeReference<T>, failonunknown: Boolean = false): T {
    if (validateJson(json)) {
        return jacksonObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failonunknown)
                .readValue<T>(json, objec)
    } else {
        throw IllegalArgumentException("Not valid json format")
    }
}


fun validateJson(json: String) : Boolean {
    return try {
        jacksonObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .readTree(json)
        true
    } catch (ex: Exception) {
        false
    }
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
