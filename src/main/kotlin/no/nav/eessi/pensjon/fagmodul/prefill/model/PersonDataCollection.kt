package no.nav.eessi.pensjon.fagmodul.prefill.model

import no.nav.eessi.pensjon.personoppslag.pdl.model.Person
import no.nav.eessi.pensjon.personoppslag.pdl.model.Sivilstandstype

data class PersonDataCollection(
        val gjenlevendeEllerAvdod: Person?,
        val forsikretPerson: Person?,
        val ektefelleBruker: Person?,
        val ekteTypeValue: Sivilstandstype?,
        val barnBrukereFraTPS: List<Person>
)