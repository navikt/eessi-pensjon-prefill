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
class InstitutionController {
    //@RequestMapping("/")

    private val logger: Logger by lazy { LoggerFactory.getLogger(InstitutionController::class.java)}

    @Autowired
    lateinit var service : InstitutionService

    @GetMapping("/institutions/{id}")
    fun getInstitutionsById(@PathVariable id: String): Institusjon? {
        logger.debug("ID :  $id   service : $service")
        val data = service.getInstitutionByID(id)
        logger.debug("data : $data")
        return data.body
    }

    @GetMapping("/institutions/bytopic/{topic}")
    fun getInstitutionsByTopic(@PathVariable("topic") topic: String?) : Institusjon?  {
        return if (topic != null) Institusjon("SE", topic) else null
    }

    @GetMapping("/institutions")
    fun getAllInstitutions() : List<Institusjon>  {
        logger.debug("ALL  service : $service")
        val data : ResponseEntity<List<Institusjon>> = service.getAllInstitutions()
        logger.debug("service datalist : $data")
        val resultat : List<Institusjon> = data.body!!
        logger.debug("result List : $resultat")
        return resultat
    }

    @GetMapping("/institutions/bycountry")
    fun getInstitutionsByCountryReqMap(@PathVariable("countryCode") countryCode: String, @PathVariable("country") country: String) : ResponseEntity.BodyBuilder {
        val institusjon = Institusjon(countryCode, country)
        return ResponseEntity.badRequest()
    }

    @GetMapping("/institutions/bycountry/{countryCode}")
    fun getInstitutionsByCountry(@PathVariable("countryCode") countryCode: String) : Institusjon {
        return Institusjon(countryCode, "Sverige")
    }

    @GetMapping("/institutions/test/")
    fun getTestAllInstitutions() : List<Institusjon> {
        val list : List<Institusjon> = service.getTestAllInstitutions().body!!
        logger.debug("list : $list")
        return list
    }

    @GetMapping("/institutions/test/{id}")
    fun getTestInstitutionsById(@PathVariable id: String) : Institusjon? {
        val response = service.getTestInstitutionByID(id).body
        logger.debug("response : $response")
        return response
    }


}