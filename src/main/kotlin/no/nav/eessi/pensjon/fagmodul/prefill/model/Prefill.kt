package no.nav.eessi.pensjon.fagmodul.prefill.model

interface Prefill<T> {

    fun prefill(prefillData: PrefillDataModel): T

}
