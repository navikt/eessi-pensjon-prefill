package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.sedmodel.PersonArbeidogOppholdUtland
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillPerson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs

class PrefillP4000(private val prefillPerson: PrefillPerson) : Prefill<SED> {

    override fun prefill(prefillData: PrefillDataModel): SED {

        prefillData.getPartSEDasJson("P4000")?.let {

            val trygdeTid = mapJsonToAny(it, typeRefs<PersonArbeidogOppholdUtland>())

            val sed = prefillPerson.prefill(prefillData)
            sed.trygdetid = trygdeTid
            prefillData.sed = sed
        }

        return prefillData.sed

    }
}