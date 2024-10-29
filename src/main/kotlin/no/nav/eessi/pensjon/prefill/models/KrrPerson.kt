package no.nav.eessi.pensjon.prefill.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class KrrPerson(
 val epostadresse: String?,
 val mobiltelefonnummer: String?
)