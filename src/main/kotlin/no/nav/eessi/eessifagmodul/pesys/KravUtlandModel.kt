package no.nav.eessi.eessifagmodul.pesys

import java.time.LocalDate


data class KravUtland(

        var mottattDato: LocalDate,
        var virkningstidspunkt: LocalDate,
        var utvandret: Boolean,
        var vurdertrygdeavtale: Boolean,
        var uttaksgrad: String,

        var utlandsopphold: List<Utlandsoppholditem>,
        var sivilstand: List<Sivilstandkilde>

)

data class Utlandsoppholditem(
        var land: String,
        var fom: LocalDate,
        var tom: LocalDate,
        var bodd: Boolean,
        var arbeidet: Boolean,
        var pensjonordningutland: String,
        var utlandid: String
)

data class Sivilstandkilde(
        var sivilstand: String,
        var from: LocalDate,
        var kilde: String
)