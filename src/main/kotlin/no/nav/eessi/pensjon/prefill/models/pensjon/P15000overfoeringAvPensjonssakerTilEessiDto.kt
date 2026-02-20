package no.nav.eessi.pensjon.prefill.models.pensjon

data class P15000overfoeringAvPensjonssakerTilEessiDto(
    val sakType: String,
    val avdod: String?,         //P15000, P6000
    val avdodMor: String?,      //P15000
    val avdodFar: String?,      //P15000
)