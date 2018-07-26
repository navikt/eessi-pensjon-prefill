package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.SED
import org.springframework.stereotype.Service

@Service
class Preutfylling(private val preutfyllingPerson: PreutfyllingPerson) {

    fun preutfyll(utfyllingData: UtfyllingData): SED {

        return when (utfyllingData.hentSED().sed)  {
            "P6000" -> {
                val personsed = preutfyllingPerson.preutfyll(utfyllingData)
                personsed
            }
            "P4000" -> {
                val personsed = preutfyllingPerson.preutfyll(utfyllingData)
                val p4000 = PreutfyllingP4000().utfyllTrygdeTid(utfyllingData)
                personsed.trygdetid = p4000
                personsed
            }
            else -> throw IllegalArgumentException("Mangler SED, eller ugyldig type SED")
        }
    }


}