package no.nav.eessi.pensjon.prefill.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class KrrPerson(
    val reservert: Boolean? = true,
    val epostadresse: String? = null,
    val mobiltelefonnummer: String? = null
) {
    companion object {
        fun String?.validateEmail(): String? {
            val emailRegex = Regex("""^[a-zA-Z0-9][a-zA-Z0-9.-]+@(([\w]+\.)+)([a-zA-Z]{2,15})$""")
            return if (this != null && emailRegex.matches(this)) this else null
        }
    }
}