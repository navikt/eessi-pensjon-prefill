package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.SED
import org.springframework.stereotype.Service

@Service
class Preutfylling(private val preutfyllingPerson: PreutfyllingPerson) {

    fun preutfyll(utfyllingData: UtfyllingData): SED {

        return when (utfyllingData.hentSEDid())  {
            "P6000" -> {
                val sed = preutfyllingPerson.preutfyll(utfyllingData)
                sed
            }
            "P4000" -> {
                //skal person data komme fra P4000? eller kun fra TPS?
                val sed = preutfyllingPerson.preutfyll(utfyllingData)
                sed.trygdetid = PreutfyllingP4000().utfyllTrygdeTid(utfyllingData)
                sed
            }
            else -> throw IllegalArgumentException("Mangler SED, eller ugyldig type SED")
        }
    }


}