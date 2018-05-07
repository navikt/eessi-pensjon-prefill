package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.services.BuCKomponentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController("BuCcontroller")
@RequestMapping("/BUC")
class BUCController {

    @Autowired
    lateinit var service : BuCKomponentService

    @GetMapping("/id")
    fun hentBuC() = service.hentEnkelBuc("3")

}