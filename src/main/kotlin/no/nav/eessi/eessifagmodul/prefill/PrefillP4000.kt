package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.PersonTrygdeTid
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs

class PrefillP4000 {

    fun utfyllTrygdeTid(utfyllingData: PrefillDataModel): PersonTrygdeTid {
        val json = utfyllingData.hentPartSedasJson("P4000")
        return mapJsonToAny(json, typeRefs<PersonTrygdeTid>())

    }

}