package no.nav.eessi.pensjon.fagmodul.prefill

interface Prefill<T> {

    fun prefill(prefillData: PrefillDataModel): T

}