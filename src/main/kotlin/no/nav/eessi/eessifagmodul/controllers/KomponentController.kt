package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jdk.nashorn.internal.parser.JSONParser
import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDResponse
import no.nav.eessi.eessifagmodul.services.EESSIKomponentenService
import org.springframework.web.bind.annotation.*
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/kontroll")
class KomponentController(val eessiKomponentenService: EESSIKomponentenService) {

    @RequestMapping("/req")
    fun testMethodBucogSED() {
        eessiKomponentenService.opprettBuCogSED ("1234", "Testeren", "12345612345")
    }

    @GetMapping("/test")
    fun testReqogRes() {
        eessiKomponentenService.opprettBuCogSED ("1234", "Testeren", "12345612345")
    }

/*
    @PostMapping("/request")
    fun testPostResponse() {
        val response = OpprettBuCogSEDResponse(
                RINASaksnummer = UUID.randomUUID().toString(),
                KorrelasjonsID = UUID.randomUUID(),
                Status = "Hallo dette er en status fra kontroller"
        )
        val mapper = jacksonObjectMapper()

        //return mapper.wr (response)

    }
*/

    @PostMapping("/opprett")
    fun opprettBuCogSED(): OpprettBuCogSEDResponse? {
        val response = eessiKomponentenService.opprettBuCogSED ("1234", "Testeren", "12345612345")
        //println(response)
        return response
    }

    @PostMapping("/postres")
    fun opprettBuCogSEDResponse(@RequestBody body: OpprettBuCogSEDResponse) {
        print(body)
    }


    @PostMapping("/postreq")
    fun opprettBuCogSEDRequest(@RequestBody body: EESSIKomponentenService.OpprettBuCogSEDRequest) {
       println(body)
    }

}