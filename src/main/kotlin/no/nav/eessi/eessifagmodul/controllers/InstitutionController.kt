package no.nav.eessi.eessifagmodul.controllers

import io.swagger.models.auth.In
import no.nav.eessi.eessifagmodul.models.Institusjon
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate

@CrossOrigin
@RestController
@RequestMapping("/institutions")
class InstitutionController(templateBuilder: RestTemplateBuilder) {

    val restTemplate: RestTemplate = templateBuilder.build()

    @GetMapping("/{id}")
    fun getInstitutionsById(@RequestParam("id") id: String) : Institusjon {
        return Institusjon("SE", "Sverige")
    }

    @GetMapping("/topic/{topic}")
    fun getInstitutionsByTopic(@RequestParam("topic") topic: String) : Institusjon  {
        return Institusjon("SE", topic)

    }

    @GetMapping("/all")
    fun getAllInstitutions() : Institusjon  {
        return Institusjon("SE", "Aalle er her!")
    }

    @GetMapping("/country/{countryCode}")
    fun getInstitutionsByCountry(@RequestParam("countryCode") countryCode: String) {

    }
}