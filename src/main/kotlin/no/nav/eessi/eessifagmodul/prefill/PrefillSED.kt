package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.SED
import org.springframework.stereotype.Service

@Service
class PrefillSED(private val prefillPerson: PrefillPerson) {

    fun prefill(utfyllingData: PrefillDataModel): SED {

        return when (utfyllingData.hentSEDid())  {
            "P2000" -> {
                val sed = prefillPerson.preutfyll(utfyllingData)
                sed.pensjon = null
                sed
            }
            "P6000" -> {
                val sed = prefillPerson.preutfyll(utfyllingData)
                sed
            }
            "P4000" -> {
                //skal person data komme fra P4000? eller kun fra TPS?
                val sed = prefillPerson.preutfyll(utfyllingData)
                sed.trygdetid = PrefillP4000().utfyllTrygdeTid(utfyllingData)
                sed
            }
            else -> throw IllegalArgumentException("Mangler SED, eller ugyldig type SED")
        }
    }


}