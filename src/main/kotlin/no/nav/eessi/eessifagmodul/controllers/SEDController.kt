package no.nav.eessi.eessifagmodul.controllers

import com.google.common.collect.Lists
import no.nav.eessi.eessifagmodul.domian.RequestException
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.services.SEDKompnentService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/sed")
class SEDController {

    @Autowired
    lateinit var sedService : SEDKompnentService

    private val logger: Logger by lazy { LoggerFactory.getLogger(SEDController::class.java) }


    @GetMapping("/createmsak/{fnr}/{saksnr}")
    fun opprettSEDmedSak(@PathVariable fnr: String, @PathVariable saksnr: String) : SED {
        println("FNR = $fnr     SAKSNR = $saksnr")
        if (fnr.isNullOrEmpty() || saksnr.isNullOrEmpty()) {
            throw RequestException("Feil ved parametere")
        }
        val sed = sedService.opprettSEDmedSak(fnr, saksnr)
        logger.debug("opprettSED fra service : $sed")
        return sed
    }

    @GetMapping("/create/{fnr}")
    fun opprettSED(@PathVariable fnr: String) : SED {
        println("FNR = $fnr")
        if (fnr.isNullOrEmpty()) {
            throw RequestException("Feil ved parametere")
        }
        val sed = sedService.opprettSED(fnr)
        logger.debug("opprettSED fra service : $sed")
        return sed
    }

    // Rute for hent gyldige SED-typer for en gitt BUC
    @GetMapping("/{bucId}")
       fun getSedsForBuc(@PathVariable bucId: String) : List<SED> {
        val sed = sedService.getSedsForBuc(bucId)
        logger.debug("opprettSED fra service : $sed")
        return Lists.newArrayList(sed)
    }


}