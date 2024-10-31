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
            val emailRegex = Regex("""[\w.-]+@[\w.-]+\.[a-zA-Z]{2,15}""")
            return if (this != null && emailRegex.matches(this)) this else null
        }
    }
}