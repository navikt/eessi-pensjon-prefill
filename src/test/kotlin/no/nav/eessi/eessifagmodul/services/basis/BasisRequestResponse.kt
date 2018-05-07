package no.nav.eessi.eessifagmodul.services.basis

interface BasisRequestResponse<T,S> {

    fun hentResponseData(request : S) : T?

}