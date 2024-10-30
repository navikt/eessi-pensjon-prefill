package no.nav.eessi.pensjon.prefill.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class KrrPerson(
 val reservert: Boolean? = true,
 val epostadresse: String? = null,
 val mobiltelefonnummer: String? = null
)