package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.PersonTrygdeTid
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs

class PrefillP4000 : Prefill<PersonTrygdeTid> {
    override fun prefill(prefillData: PrefillDataModel): PersonTrygdeTid {
        val json = prefillData.getPartSEDasJson("P4000")
        return mapJsonToAny(json, typeRefs())
    }
}