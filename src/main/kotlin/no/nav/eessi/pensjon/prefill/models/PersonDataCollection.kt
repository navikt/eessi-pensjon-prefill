package no.nav.eessi.pensjon.prefill.models

import no.nav.eessi.pensjon.personoppslag.pdl.model.PdlPerson
import no.nav.eessi.pensjon.personoppslag.pdl.model.Sivilstandstype

data class PersonDataCollection(
        val gjenlevendeEllerAvdod: PdlPerson?,
        val forsikretPerson: PdlPerson?,
        val ektefellePerson: PdlPerson? = null,
        val sivilstandstype: Sivilstandstype? = null,
        val barnPersonList: List<PdlPerson> = emptyList()
)