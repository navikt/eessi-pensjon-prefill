package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs

// SED class main request class to basis
// Strukturerte Elektroniske Dokumenter
data class SED(
        var sed: String? = null,
        var sedGVer: String? = null,
        var sedVer: String? = null,
        var nav: Nav? = null,
        var pensjon: Pensjon? = null,
        var trygdetid: PersonTrygdeTid? = null,
        var ignore: Ignore? = null
) {
    fun toJson(): String {
        return mapAnyToJson(this, true)
    }

    companion object {
        @JvmStatic
        fun create(name: String): SED {
            return SED(sed = name, sedVer = "0", sedGVer = "4")
        }

        @JvmStatic
        fun fromJson(sed: String): SED {
            return mapJsonToAny(sed, typeRefs(), true)
        }
    }
}

//Data struktur for bruk av apirequest(frontend) og utfyllingdata (backend)
data class InstitusjonItem(
        var country: String? = null,
        var institution: String? = null
)

enum class SEDType {
    P2000,
    P2100,
    P2200,
    P3000,
    P4000,
    P6000,
    P5000,
    P7000;
}