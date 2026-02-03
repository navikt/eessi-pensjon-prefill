package no.nav.eessi.pensjon.prefill.models.pensjon

import java.time.LocalDate

abstract class YtelsePerMndBase(
    open val fom: LocalDate? = null,
    open val belop: Int? = null,
    open val ytelseskomponentListe: List<Ytelseskomponent>? = null
)