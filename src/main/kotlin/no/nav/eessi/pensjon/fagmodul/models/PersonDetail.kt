package no.nav.eessi.pensjon.fagmodul.models

import java.time.LocalDate

data class PersonDetail(
        var sakType: String? = null,
        var buc: String? = null,
        var personNavn: String? = null,
        var kjoenn: String? = null,
        var fnr: String? = null,
        var fodselDato: LocalDate? = null,
        var aar16Dato: LocalDate? = null,
        var alder: Int? = null,
        var aktoerId: String? = null,
        var sivilStand: String? = null,
        var persomStatus: String? = null,
        var euxCaseId: String? = null
)