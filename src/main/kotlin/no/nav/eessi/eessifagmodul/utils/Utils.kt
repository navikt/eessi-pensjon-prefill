package no.nav.eessi.eessifagmodul.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClientException
import java.text.SimpleDateFormat
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


enum class SedEnum (val sed: String) {
    P2000("P2000"),
    P2100("P2100"),
    P2200("P2200"),
    P4000("P4000"),
    P6000("P6000"),
    P5000("P5000"),
    P7000("P7000");
    fun valid(sed: String): Boolean {
        return sed == this.sed
    }
}

//andre sed..
val STANDARD_SED = sedEnumToString()

fun sedEnumToString(): String {
    val strb = StringBuilder()
    SedEnum.values().toList().forEach {
        strb.append(it.sed).append(",")
    }
    strb.deleteCharAt(strb.length-1)
    return strb.toString()
}

fun validsed(sed: String, validsed: String) : Boolean {
    val result: List<String> = validsed.split(",").map { it.trim() }
    return result.contains(sed)
}

fun XMLGregorianCalendar.simpleFormat(): String {
    return SimpleDateFormat("YYYY-MM-dd").format(this.toGregorianCalendar().time)
}

