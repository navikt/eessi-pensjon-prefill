package no.nav.eessi.pensjon.prefill.models.pensjon

data class P8000AnmodningOmTilleggsinformasjon(
    val sakType: String,
    val harKravhistorikkGjenlevende: Boolean, // tidligere brukersSakListe.kravhistorikk
)
