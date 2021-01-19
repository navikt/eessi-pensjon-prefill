package no.nav.eessi.pensjon.fagmodul.prefill.model

import no.nav.eessi.pensjon.personoppslag.pdl.model.Person

data class PersonDataCollection(
        val gjenlevendeEllerAvdod: Person?,
        val forsikretPerson: Person?,
        val ektefelleBruker: Person?,
        val ekteTypeValue: String?,
        val barnBrukereFraTPS: List<Person>?
)