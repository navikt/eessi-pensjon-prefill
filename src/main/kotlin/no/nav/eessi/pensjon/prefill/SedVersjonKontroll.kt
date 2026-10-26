package no.nav.eessi.pensjon.prefill

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

/**
 * Logger ERROR når en prefylt SED har en annen sedVer enn forventet (EESSI_SED_VERSJON fra nais-config).
 */
@ControllerAdvice(assignableTypes = [PrefillController::class])
class SedVersjonKontroll(@Value("\${EESSI_SED_VERSJON}") private val forventetSedVersjon: String) : ResponseBodyAdvice<Any> {

    private val logger = LoggerFactory.getLogger(SedVersjonKontroll::class.java)
    private val mapper = jacksonObjectMapper()

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>) =
        returnType.parameterType == String::class.java

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? = body.also { if (it is String) kontrollerSedVersjon(it) }

    fun kontrollerSedVersjon(sedJson: String): Boolean {
        val sed = try {
            mapper.readTree(sedJson)
        } catch (_: JsonProcessingException) {
            null
        }
        if (sed == null) {
            logger.error("Klarte ikke å lese SED: feilaktig eller tom JSON")
            return false
        }
        val sedVer = sed.get("sedVer")?.takeUnless { it.isNull }?.asText()
        if (sedVer == forventetSedVersjon) return true

        logger.error("Prefylt SED ${sed.get("sed")?.asText()} har sedVer=$sedVer, forventet $forventetSedVersjon")
        return false
    }
}
