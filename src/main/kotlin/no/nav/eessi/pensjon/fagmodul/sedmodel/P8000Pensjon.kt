package no.nav.eessi.pensjon.fagmodul.sedmodel

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

class P8000Pensjon(
    val anmodning: AnmodningOmTilleggsInfo? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AnmodningOmTilleggsInfo(
    val referanseTilPerson: String? = null
)