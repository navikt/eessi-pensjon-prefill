package no.nav.eessi.eessifagmodul.pesys

import no.nav.eessi.eessifagmodul.models.StatsborgerskapItem
import java.time.LocalDate

//Model for opprettelse av kravhode til pesys.
//Krever p2x00 og P3000_no, P4000,P5000 før en kan
//tilby tjeneste med nødvendig informasjon for PESYS til å opprette kravhode automatisk.

//omhandler alle SED?
data class KravUtland(
        var avsenderLand: String? = null,
        var mottattDato: LocalDate? = null,
        var virkningsTidspunkt: LocalDate? = null,
        var utvandret: Boolean? = null,
        var vurdertrygdeavtale: Boolean? = null,
        var uttaksgrad: String? = null,
        var utlandsopphold: List<Utlandsoppholditem>? = null,
        var sivilstand: Sivilstandkilde? = null,
        var statsborgerskap: StatsborgerskapItem? = null
)

data class Utlandsoppholditem(
        var land: String? = null,
        var fom: LocalDate? = null,
        var tom: LocalDate? = null,
        var bodd: Boolean? = null,
        var arbeidet: Boolean? = null,
        var pensjonordningutland: PensjonOrdningUtland? = null,
        var utlandPin: String? = null
)

data class PensjonOrdningUtland(
        var ordning: String? = null
)

data class Sivilstandkilde(
        var sivilstand: String? = null,
        var fom: LocalDate? = null,
        var tom: LocalDate? = null,
        var kilde: String? = null
)
