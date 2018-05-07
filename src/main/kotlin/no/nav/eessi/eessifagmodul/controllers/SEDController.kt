package no.nav.eessi.eessifagmodul.controllers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/SED")
class SEDController {

    private val logger: Logger by lazy { LoggerFactory.getLogger(SEDController::class.java) }

    @GetMapping("/create")
    fun opprettSED(@RequestParam fnr: String, @RequestParam saksnr: String) {
        println("Hello from /create")
    }

    // Rute for hent gyldige SED-typer for en gitt BUC
    @GetMapping("/{bucId}")
    fun getSedsForBuc(@RequestParam bucId: String) {

    }
}