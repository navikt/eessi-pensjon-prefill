package no.nav.eessi.eessifagmodul.models

import com.fasterxml.jackson.annotation.JsonProperty

/*
//generere et P6000 json objekt med Nav og Pensjon objekt.
data class P6000 (
        var nav: Nav? = null,
        val sed: String? = null,
        val sedGVer: String? = null,
        val sedVer: String? = null,
        @JsonProperty("Sector Components/Pensions/P6000 ")
        val sector: String? = null,
        var pensjon: Pensjon? = null,
        val ignore: Ignore? = null
)
*/

//data class SED (
//        var nav: Nav? = null,
//        val sed: String? = null,
//        val sedGVer: String? = null,
//        val sedVer: String? = null,
//        //@JsonProperty("Sector Components/Pensions/P6000 ")
//        //val sector: String? = null,
//        var pensjon: Pensjon? = null,
//        val ignore: Ignore? = null
//)

class SedMock {

    fun genererP6000Mock(): SED {
        return SED (
                nav = NavMock().genererNavMock(),
                pensjon = PensjonMock().genererMockData(),
                sed = "P6000",
                sedVer = "0",
                sedGVer = "4"
                //sector = "Sector Components/Pensions/P6000"
        )
    }

    fun genererEmptyP6000Mock(): SED {
        return SED (
                nav = Nav(),
                pensjon = Pensjon(),
                sed = "P6000",
                sedVer = "0",
                sedGVer = "4"
                //sector = "Sector Components/Pensions/P6000"
        )
    }
    fun genererEmptyP2000Mock(): SED {
        return SED (
                nav = Nav(),
                pensjon = Pensjon(),
                sed = "P2000",
                sedVer = "0",
                sedGVer = "4"
                //sector = "Sector Components/Pensions/P2000"
        )
    }



}