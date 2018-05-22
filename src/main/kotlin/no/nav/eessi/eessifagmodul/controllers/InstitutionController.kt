package no.nav.eessi.eessifagmodul.controllers

import com.google.common.collect.Lists
import no.nav.eessi.eessifagmodul.models.Institusjon
import no.nav.eessi.eessifagmodul.services.EESSIKomponentenService
import no.nav.eessi.eessifagmodul.services.InstitutionService
import org.jetbrains.annotations.TestOnly
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
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
        val data = service.getInstitutionByID(id)
        logger.debug("data : $data")
        return data.body
    }

    @GetMapping("/bytopic/{topic}")
    fun getInstitutionsByTopic(@PathVariable("topic") topic: String?) : Institusjon?  {
        logger.debug("Topic : $topic")
        if (topic.isNullOrBlank()) {
            logger.error("ERROR: Topic Null or Blank")
            return getErrorInstitusjon("ERROR: Topic Null or Blank")
        }
        var response: ResponseEntity<Institusjon>?
        try {
            response = service.getInstitutionsByTopic(topic)
            logger.debug("Response : $response")
        } catch (ext : Exception) {
            logger.error("Error : ${ext.message}")
            return getErrorInstitusjon("ERROR : ${ext.message}")
        }
        if (response.statusCode != HttpStatus.OK) {
            return getErrorInstitusjon("ERROR : ${response.statusCode}")
        }
        return response.body
    }

    fun getErrorInstitusjon(errMsg: String = "ERROR") : Institusjon {
        return Institusjon("ERROR", errMsg)
    }

    @GetMapping("")
    fun getAllInstitutions() : List<Institusjon>  {
        logger.debug("ALL  service : $service")
        val data : ResponseEntity<List<Institusjon>> = service.getAllInstitutions()
        logger.debug("service datalist : $data")
        val resultat : List<Institusjon> = data.body!!
        logger.debug("result List : $resultat")
        return resultat
    }

    @GetMapping("/bycountry")
    fun getInstitutionsByCountryReqMap(@PathVariable("countryCode") countryCode: String, @PathVariable("country") country: String) : ResponseEntity.BodyBuilder {
        //val institusjon = Institusjon(countryCode, country)
        return ResponseEntity.badRequest()
    }

    @GetMapping("/bycountry/{countryCode}")
    fun getInstitutionsByCountry(@PathVariable("countryCode") countryCode: String) : Institusjon {
        return Institusjon(countryCode, "Sverige")
    }

    @GetMapping("/noen")
    fun getInstitutionsNoen(@PathVariable("id") id: Int, @PathVariable("noe") noe: String) : Institusjon {
        return service.getInstitutionsNoen(id, noe)
    }


    @TestOnly
    @GetMapping("/test/")
    fun getTestAllInstitutions() : List<Institusjon> {
        val list : List<Institusjon> = service.getTestAllInstitutions().body!!
        logger.debug("list : $list")
        return list
    }

    @TestOnly
    @GetMapping("/test/{id}")
    fun getTestInstitutionsById(@PathVariable id: String) : Institusjon? {
        val response = service.getTestInstitutionByID(id).body
        logger.debug("response : $response")
        return response
    }


}