package no.nav.eessi.pensjon.fagmodul.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

//Data struktur for bruk av apirequest(frontend) og utfyllingdata (backend)
@JsonIgnoreProperties(ignoreUnknown = true)
data class InstitusjonItem(
        var country: String,
        var institution: String,
        var name: String? = null,
        var buc: List<String>? = null) {

        //sjekker på Instisjon legger ut ID til rina som <XX:ZZZZZ>
        fun checkAndConvertInstituion(): String {
            if (institution.contains(":")) {
                return institution
            }
            return "$country:$institution"

        }
        fun checkBuc(bucItem: String) = buc?.contains(bucItem)
}