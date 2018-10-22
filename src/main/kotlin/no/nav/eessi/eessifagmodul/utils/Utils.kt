package no.nav.eessi.eessifagmodul.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClientException
import javax.xml.datatype.XMLGregorianCalendar

inline fun <reified T : Any> typeRef(): ParameterizedTypeReference<T> = object : ParameterizedTypeReference<T>() {}
inline fun <reified T : Any> typeRefs(): TypeReference<T> = object : TypeReference<T>() {}
inline fun <reified T : Any> mapJsonToAny(json: String, objec : TypeReference<T>, failonunknown: Boolean = false): T {
    if (validateJson(json)) {
        return jacksonObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failonunknown)
                .readValue<T>(json, objec)
    } else {
        throw IllegalArgumentException("Not valid json format")
    }
}

fun createErrorMessage(responseBody: String): RestClientException {
    return mapJsonToAny(responseBody, typeRefs())
}

fun mapAnyToJson(data: Any): String {
    return  jacksonObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(data)
}
fun mapAnyToJson(data: Any, nonempty: Boolean = false): String {
    return if (nonempty) {

        val list = mutableListOf<String>()

        list.add("2adfgadfg ")

        val json = jacksonObjectMapper()
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(data)
        json
    } else {
        mapAnyToJson(data)
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

enum class SedEnum {
    P2000,
    P2100,
    P2200,
    P3000,
    P4000,
    P6000,
    P5000,
    P7000;
}

//andre sed..
const val START_SED = "P2000,P2100,P2200"
const val STANDARD_SED = "P3000,P5000,P6000,P7000"
const val ALL_SED = "P2000,P2100,P2200,P3000,P4000,P6000,P5000,P7000"

fun validsed(sed: String, validsed: String) : Boolean {
    val result: List<String> = validsed.split(",").map { it.trim() }
    return result.contains(sed)
}

fun XMLGregorianCalendar.simpleFormat(): String {
    //return SimpleDateFormat("YYYY-MM-dd").format(this.toGregorianCalendar().time)
    return this.toString().substring (0, 10)
}
