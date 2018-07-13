package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.clients.aktoerid.AktoerIdClient
import no.nav.eessi.eessifagmodul.models.FrontendRequest
import no.nav.eessi.eessifagmodul.models.createSED
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class Preutfylling {

    @Autowired
    private lateinit var aktoerIdClient: AktoerIdClient

    @Autowired
    private lateinit var preutfyllingNav: PreutfyllingNav

    @Autowired
    private lateinit var preutfyllingPensjon: PreutfyllingPensjon

    private val logger: Logger by lazy { LoggerFactory.getLogger(Preutfylling::class.java) }

    private fun hentPinIdentFraAktorid(pin: String? = ""): String? {
        return aktoerIdClient.hentIdentForAktoerId(aktoerId = pin ?: "" )?.ident
    }

    fun preutfylling(request: FrontendRequest) : UtfyllingData   {
        logger.debug("----------------- Preutfylling START ----------------- ")
        if (request.sed == null || request.caseId == null) {
            throw IllegalArgumentException("Mangler SED og CaseID")
        }
        logger.debug("Preutfylling NAV :  $preutfyllingNav   og Pensjon : $preutfyllingPensjon")

        val sed = createSED(request.sed)
        logger.debug("Preutfylling SED : $sed")

        //har vi hentet ned fnr fra aktor?
        val pinid = hentPinIdentFraAktorid(request.pinid)
        logger.debug("Preutfylling hentet pinid fra Aktoeridclent.")

        val utfylling = UtfyllingData(sed, request, pinid!!)
        logger.debug("Preutfylling UtfyllingData")

        sed.nav = preutfyllingNav.utfyllNav(utfylling)
        logger.debug("Preutfylling Utfylling NAV")

        sed.pensjon = preutfyllingPensjon.pensjon(utfylling)
        logger.debug("Preutfylling Utfylling Pensjon")

        val json = mapAnyToJson(utfylling.sed)
        logger.debug("Detaljinfo om preutfylt SED: $json")

        logger.debug("----------------- Preutfylling END ----------------- ")
        return utfylling
    }


}

