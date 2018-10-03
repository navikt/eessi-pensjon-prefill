package no.nav.eessi.eessifagmodul.models

// SED class main request class to basis
// Strukturerte Elektroniske Dokumenter
data class SED (
        var sed: String? = null,
        val sedGVer: String? = null,
        val sedVer: String? = null,
        var nav: Nav? = null,
        var pensjon: Pensjon? = null,
        var trygdetid: PersonTrygdeTid? = null,
        val ignore: Ignore? = null
)

//Data struktur for bruk av apirequest(frontend) og utfyllingdata (backend)
data class InstitusjonItem(
        val country: String? = null,
        val institution: String? = null
)

//Oppretter ny sed ut ifra navn (P2000-P4000-P5000-osv..)
fun createSED(sedName: String): SED {
    return SED (
        sed = sedName,
        sedVer = "0",
        sedGVer = "4"
    )
}
