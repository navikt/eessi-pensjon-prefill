package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs

// SED class main request class to basis
// Strukturerte Elektroniske Dokumenter
data class SED(
        var sed: String? = null,
        val sedGVer: String? = null,
        val sedVer: String? = null,
        var nav: Nav? = null,
        var pensjon: Pensjon? = null,
        var trygdetid: PersonTrygdeTid? = null,
        val ignore: Ignore? = null
) {
    fun toJson(): String {
        return mapAnyToJson(this, true)
    }

    companion object {
        @JvmStatic fun create(name: String): SED {
            return SED(sed = name, sedVer = "0", sedGVer = "4")
        }
        @JvmStatic fun fromJson(sed: String): SED {
            return mapJsonToAny(sed, typeRefs(), true)
        }
    }
}

//Data struktur for bruk av apirequest(frontend) og utfyllingdata (backend)
data class InstitusjonItem(
        val country: String? = null,
        val institution: String? = null
)
