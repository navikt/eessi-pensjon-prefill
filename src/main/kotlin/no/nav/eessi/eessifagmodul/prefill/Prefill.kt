package no.nav.eessi.eessifagmodul.prefill

interface Prefill<T> {

    fun prefill(prefillData: PrefillDataModel): T

}