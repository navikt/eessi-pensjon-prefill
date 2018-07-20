package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.clients.aktoerid.AktoerIdClient
import no.nav.eessi.eessifagmodul.models.SED
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PreutfyllingPerson(private val aktoerIdClient: AktoerIdClient, private val preutfyllingNav: PreutfyllingNav, private val preutfyllingPensjon: PreutfyllingPensjon) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PreutfyllingPerson::class.java) }


    private fun hentPinIdentFraAktorid(pin: String? = ""): String? {
        logger.debug("henter pinid fra aktoerid $pin")
        return try {
            aktoerIdClient.hentIdentForAktoerId(aktoerId = pin ?: "" )?.ident
        } catch (ex: Exception) {
            logger.error(ex.message)
            null
        }
    }

    //hva skal returnere sed? eller utfyllingData?
    //
    //Validering hva gjør vi med manglende data inn?
    fun preutfyll(utfyllingData: UtfyllingData): SED {

        logger.debug("utfyllingData: $utfyllingData")

        logger.debug("----------------- Preutfylling START ----------------- ")

        logger.debug("Preutfylling NAV     : ${preutfyllingNav::class.java} ")
        logger.debug("Preutfylling Pensjon : ${preutfyllingPensjon::class.java} ")

        val sed = utfyllingData.hentSED()
        //val sed = createSED(request.sed)
        //logger.debug("Preutfylling SED : $sed")

        //har vi hentet ned fnr fra aktor?
        utfyllingData.putPinID(hentPinIdentFraAktorid(utfyllingData.hentAktoerid()))
        logger.debug("Preutfylling hentet pinid fra aktoerIdClient.")

        //val utfylling = UtfyllingData(sed, request, pinid)

        logger.debug("Preutfylling Utfylling Data")

        sed.nav = preutfyllingNav.utfyllNav(utfyllingData)
        logger.debug("Preutfylling Utfylling NAV")

        sed.pensjon = preutfyllingPensjon.pensjon(utfyllingData)
        logger.debug("Preutfylling Utfylling Pensjon")

        logger.debug("----------------- Preutfylling END ----------------- ")

        return utfyllingData.hentSED()
    }

}

