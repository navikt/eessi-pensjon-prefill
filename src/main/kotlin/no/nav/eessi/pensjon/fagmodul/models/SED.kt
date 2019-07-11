package no.nav.eessi.pensjon.fagmodul.models

import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs

// SED class main request class to basis
// Strukturerte Elektroniske Dokumenter
data class SED(
        var sed: String? = null,
        var sedGVer: String? = null,
        var sedVer: String? = null,
        var nav: Nav? = null,
        var pensjon: Pensjon? = null,
        var trygdetid: PersonArbeidogOppholdUtland? = null, //P4000
        var ignore: Ignore? = null,

        //H120
        var horisontal: Horisontal? = null
) {
    fun toJson(): String {
        return mapAnyToJson(this, false)
    }

    fun toJsonSkipEmpty(): String {
        return mapAnyToJson(this, true)
    }


    companion object {
        @JvmStatic
        fun create(name: String): SED {
            return SED(sed = name, sedVer = "1", sedGVer = "4")
        }

        @JvmStatic
        fun fromJson(sed: String): SED {
            return mapJsonToAny(sed, typeRefs(), true)
        }
    }

    override fun toString() : String = this.toJson()

}