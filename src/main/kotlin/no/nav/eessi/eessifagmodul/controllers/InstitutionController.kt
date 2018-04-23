package no.nav.eessi.eessifagmodul.controllers

import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/institutions")
class InstitutionController {

    @GetMapping("/{id}")
    fun getInstitutionsById(@RequestParam("id") id: String) {

    }

    @GetMapping("/topic/{topic}")
    fun getInstitutionsByTopic(@RequestParam("topic") topic: String) {

    }

    @GetMapping("/country/{countryCode}")
    fun getInstitutionsByCountry(@RequestParam("countryCode") countryCode: String) {

    }
}