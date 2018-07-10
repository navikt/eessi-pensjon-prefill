package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.utils.logger
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Preutfylling{

    private val logger: Logger by lazy { LoggerFactory.getLogger(Preutfylling::class.java) }


    fun preutfyllingAvSED(sed: SED) : SED    {

        //hente data fra PEN
        //mappe om XML/data til onbje
        //grad av utfylling?

        val utfylling = Utfylling(sed)

        sed.nav = PreutfyllingNav(utfylling).utfyllNav()

        sed.pensjon = PreutfyllingPensjon(utfylling).pensjon()

        return sed
    }

    fun preutfylling(sed: SED) : Utfylling   {


        val utfylling = Utfylling(sed = sed)

        sed.nav = PreutfyllingNav(utfylling).utfyllNav()

        sed.pensjon = PreutfyllingPensjon(utfylling).pensjon()

        logger.debug("NAV     : ${sed.nav}")
        logger.debug("Pensjon : ${sed.pensjon}")

        return utfylling

    }

}

fun validSED(sedName: String, sed: SED):Boolean {
    return sedName === sed.sed
}


class Utfylling(val sed: SED) {

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
