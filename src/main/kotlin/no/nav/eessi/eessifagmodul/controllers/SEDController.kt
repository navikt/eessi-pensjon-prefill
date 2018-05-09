package no.nav.eessi.eessifagmodul.controllers

import com.google.common.collect.Lists
import no.nav.eessi.eessifagmodul.models.NavPerson
import no.nav.eessi.eessifagmodul.models.SED
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/sed")
class SEDController {

    private val logger: Logger by lazy { LoggerFactory.getLogger(SEDController::class.java) }

    @GetMapping("/create/{fnr]{saksnr}")
    fun opprettSED(@PathVariable fnr: String, @PathVariable saksnr: String) : SED {
        val navperson = NavPerson(fnr)
        val sed = SED("SEDtype", "SAKnr: $saksnr", navperson,null, null )
        return sed
    }

    // Rute for hent gyldige SED-typer for en gitt BUC
    @GetMapping("/{bucId}")
       fun getSedsForBuc(@PathVariable bucId: String) : List<SED> {
        val navperson = NavPerson(null)
        val sed = SED("SEDtype", null, navperson,null, null )
        return Lists.newArrayList(sed)
    }


}