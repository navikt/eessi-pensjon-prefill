package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.PersonTrygdeTid
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs
import org.springframework.stereotype.Component

@Component
class PrefillP4000(private val prefillPerson: PrefillPerson) : Prefill<SED> {

    override fun prefill(prefillData: PrefillDataModel): SED {

        val json = prefillData.getPartSEDasJson("P4000")

        val trygdeTid = mapJsonToAny(json, typeRefs<PersonTrygdeTid>())

        val sed = prefillPerson.prefill(prefillData)
        sed.trygdetid = trygdeTid
        prefillData.sed = sed
        return prefillData.sed

    }
}