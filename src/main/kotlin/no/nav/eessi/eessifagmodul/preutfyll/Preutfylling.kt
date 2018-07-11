package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Preutfylling{

    private val logger: Logger by lazy { LoggerFactory.getLogger(Preutfylling::class.java) }

    fun preutfylling(sed: SED) : Utfylling   {

        val utfylling = Utfylling(sed = sed, saksnr = 1234)
        sed.nav = PreutfyllingNav(utfylling).utfyllNav()
        sed.pensjon = PreutfyllingPensjon(utfylling).pensjon()

        val json = mapAnyToJson(utfylling.sed)
        logger.debug("Detaljinfo om preutfylt SED: $json")

        return utfylling
    }


}

class Utfylling(val sed: SED, val saksnr: Int, val buc: String = "") {

    private val logger: Logger by lazy { LoggerFactory.getLogger(Utfylling::class.java) }

    val grad = mutableListOf<Grad>()
    val tjenester = mutableListOf<String>()
    val beskrivelse: String = ""

    fun leggtilGrad(item: Grad) {
        logger.debug("Legger til grad : $item   grad-verdi :  ${item.grad} ")
        grad.add(item)
    }

    fun leggtilTjeneste(item: String) {
        tjenester.add(item)
    }

}

data class Grad(
        var grad: Int? = null,
        var felt: String? = null,
        var beskrivelse: String? =null
)
