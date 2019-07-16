package no.nav.eessi.pensjon.fagmodul.eux

import no.nav.eessi.pensjon.fagmodul.sedmodel.Krav

//P15000-P2100 levende PinID og Krav
data class PinOgKrav(
        val fnr: String? = null,
        val krav: Krav? = null
)
