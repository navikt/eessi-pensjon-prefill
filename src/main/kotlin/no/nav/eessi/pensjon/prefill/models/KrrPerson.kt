package no.nav.eessi.pensjon.prefill.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class DigitalKontaktinfoBolk(
    val feil: Map<String, String>? = null,
    val personer: Map<String, DigitalKontaktinfo>? = null
)

data class DigitalKontaktinfo(
    val epostadresse: String? = null,
    val aktiv: Boolean,
    val kanVarsles: Boolean? = null,
    val reservert: Boolean? = null,
    val mobiltelefonnummer: String? = null,
    val personident: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KrrPerson(
    val reservert: Boolean? = true,
    val epostadresse: String? = null,
    val mobiltelefonnummer: String? = null
) {
    companion object {
        fun String?.validateEmail(processDefinitionVersion: String? = null): String? {
            // ingen validering på epost etter versjon 4.3
            if (processDefinitionVersion?.contains("4.3") == true) return this
            val emailRegex = Regex("""^[a-zA-Z0-9][a-zA-Z0-9.-]+@(([\w]+\.)+)([a-zA-Z]{2,15})$""")
            return if (this != null && emailRegex.matches(this)) this else null
        }
    }
}