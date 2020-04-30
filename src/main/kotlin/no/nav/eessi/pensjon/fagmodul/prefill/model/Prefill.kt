package no.nav.eessi.pensjon.fagmodul.prefill.model

import no.nav.eessi.pensjon.fagmodul.sedmodel.SED

interface Prefill {

    fun prefill(prefillData: PrefillDataModel): SED

}
