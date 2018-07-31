package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.EessisakItem
import no.nav.eessi.eessifagmodul.models.PersonTrygdeTid
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs

class PreutfyllingP4000 {

    fun utfyllTrygdeTid(utfyllingData: UtfyllingData): PersonTrygdeTid {

        val json = utfyllingData.hentPartSedasJson("P4000")!!
        val trygdeTid = mapJsonToAny(json, typeRefs<PersonTrygdeTid>())

        return trygdeTid
    }

}