package no.nav.eessi.pensjon.prefill.models

import no.nav.eessi.pensjon.personoppslag.pdl.model.Person
import no.nav.eessi.pensjon.personoppslag.pdl.model.Sivilstandstype

data class PersonDataCollection(
        val gjenlevendeEllerAvdod: Person?,
        val forsikretPerson: Person?,
        val ektefellePerson: Person? = null,
        val sivilstandstype: Sivilstandstype? = null,
        val barnPersonList: List<Person> = emptyList()
)