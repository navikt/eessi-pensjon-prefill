package no.nav.eessi.eessifagmodul.controllers

import com.google.common.collect.Lists
import no.nav.eessi.eessifagmodul.models.Institusjon
import no.nav.eessi.eessifagmodul.services.EESSIKomponentenService
import no.nav.eessi.eessifagmodul.services.InstitutionService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/institutions")
class InstitutionController {

    private val logger: Logger by lazy { LoggerFactory.getLogger(InstitutionController::class.java)}

    @Autowired
    lateinit var service : InstitutionService

    @GetMapping("/byid/{id}")
    fun getInstitutionsById(@PathVariable id: String): Institusjon? {
        logger.debug("ID :  $id   service : $service")
        return service.getInstitutionByID(id)
    }

    @GetMapping("/bytopic/{topic}")
    fun getInstitutionsByTopic(@PathVariable("topic") topic: String?) : Institusjon?  {
        return if (topic != null) Institusjon("SE", topic) else null
    }

    @GetMapping("/all")
    fun getAllInstitutions() : List<Institusjon>?  {
        logger.debug("ALL  service : $service")
        return service.getAllInstitutions()
    }

    @RequestMapping("/bycountry")
    fun getInstitutionsByCountryReqMap(@RequestParam("countryCode", defaultValue = "SE" ) countryCode: String) : ResponseEntity.BodyBuilder {
        return ResponseEntity.badRequest()
    }

    @GetMapping("/bycountry/{countryCode}")
    fun getInstitutionsByCountry(@PathVariable("countryCode") countryCode: String) : Institusjon {
        return Institusjon(countryCode, "Sverige")
    }
}