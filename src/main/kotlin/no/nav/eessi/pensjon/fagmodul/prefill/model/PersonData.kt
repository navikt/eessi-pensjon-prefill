package no.nav.eessi.pensjon.fagmodul.prefill.model

import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker

data class PersonData(
        val gjenlevendeEllerAvdod: Bruker?,
        val forsikretPerson: Bruker,
        val ektefelleBruker: Bruker?,
        val ekteTypeValue: String?,
        val barnBrukereFraTPS: List<Bruker>
)