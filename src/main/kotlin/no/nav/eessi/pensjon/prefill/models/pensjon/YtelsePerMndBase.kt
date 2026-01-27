package no.nav.eessi.pensjon.prefill.models.pensjon

import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto.Ytelseskomponent
import java.time.LocalDate

abstract class YtelsePerMndBase(
    open val fom: LocalDate? = null,
    open val belop: Int? = null,
    open val ytelseskomponentListe: List<Ytelseskomponent>? = null
)