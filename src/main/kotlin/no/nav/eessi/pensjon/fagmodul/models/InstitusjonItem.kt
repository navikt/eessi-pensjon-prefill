package no.nav.eessi.pensjon.fagmodul.models

//Data struktur for bruk av apirequest(frontend) og utfyllingdata (backend)
data class InstitusjonItem(
        var country: String,
        var institution: String,
        var name: String? = null) {

        //sjekker på Instisjon legger ut ID til rina som <XX:ZZZZZ>
        fun checkAndConvertInstituion(): String {
            if (institution.contains(":")) {
                return institution
            }
            return "$country:$institution"
        }
}