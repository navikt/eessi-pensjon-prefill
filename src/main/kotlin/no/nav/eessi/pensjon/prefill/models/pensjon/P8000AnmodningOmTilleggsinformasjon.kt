package no.nav.eessi.pensjon.prefill.models.pensjon

data class P8000AnmodningOmTilleggsinformasjon(
    val sakType: EessiFellesDto.EessiSakType,
    val harKravhistorikkGjenlevende: Boolean, // tidligere brukersSakListe.kravhistorikk
)
