package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.jaxws.client.AktoerIdClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/ping")
class PingController {

    @Autowired
    lateinit var aktoerIdClient: AktoerIdClient

    @GetMapping("/")
    fun getPing(): ResponseEntity<Unit> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/testAktoer/{ident}")
    fun testAktoer(@PathVariable("ident") ident: String, @RequestHeader(name = "Authorization") authorizationHeader: String): String? {
        return aktoerIdClient.hentAktoerIdForIdent(ident, authorizationHeader.replace("Bearer ", ""))?.aktoerId
    }
}
