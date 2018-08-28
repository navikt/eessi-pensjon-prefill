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
    	//val medlemskap: Medlemskap? = null, A01?.
        //var bekreftforsikred: BekreftelseBosettingsperiode? = null,
        val ignore: Ignore? = null
)

//data struktir for bruk av apireqiuest(frontend) og utfyllingdata)
data class InstitusjonItem(
        val country: String? = null,
        val institution: String? = null
)
//Oppretter ny sed ut ifra navn (P2000-P4000-P5000)
//Hører denne til her eller i Utils?
fun createSED(sedName: String?): SED {
    return SED (
        sed = sedName,
        //legge ut som attributes? istede for hardkodet?
        sedVer = "0",
        //legge ut som attributes? istede for hardkodet?
        sedGVer = "4"
    )
}
