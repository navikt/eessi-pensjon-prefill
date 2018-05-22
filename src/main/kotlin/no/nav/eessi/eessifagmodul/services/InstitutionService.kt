package no.nav.eessi.eessifagmodul.services

import com.google.common.collect.Lists
import io.swagger.models.auth.In
import no.nav.eessi.eessifagmodul.domian.RequestException
import no.nav.eessi.eessifagmodul.models.Institusjon
import org.jetbrains.annotations.TestOnly
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI

@Service
class InstitutionService {

    private val logger: Logger = LoggerFactory.getLogger(InstitutionService::class.java)

    //BASIS urlpath
    val path : String = "/cpi"

    @Autowired
    lateinit var rest: EESSIRest

    init {
        logger.debug("Starter: InstitutionService")
    }

    fun getInstitutionByID(id : String) : ResponseEntity<Institusjon> {
        val response = rest.restTemplate.exchange(rest.createGet("$path/getInstitution/$id"), rest.typeRef<Institusjon>())
        logger.debug("ResponseEntity : $response")
        return response
    }

    fun getAllInstitutions() : ResponseEntity<List<Institusjon>>  {
        val responseEntity =  rest.restTemplate.exchange(rest.createGet("$path/getInstitutions"), rest.typeRef<List<Institusjon>>())
        logger.debug("ResponseEntity : $responseEntity")
        return responseEntity
    }

    fun getInstitutionsByTopic(topic : String?) : ResponseEntity<Institusjon> {
        logger.debug("Topic : $topic")
        if (topic.isNullOrBlank()) {
            logger.error("Topic is null or blank")
            throw IllegalArgumentException()
        }
        logger.error("Topic : $topic")
        var response : ResponseEntity<Institusjon>? = null
        try {
            response = rest.restTemplate.exchange(rest.createGet("$path/getInstitution/bytopic/$topic"), rest.typeRef<Institusjon>())
        } catch (ex : Exception) {
           throw IllegalArgumentException("Error : ${ex.message}")
        }
        logger.debug("ResponseEntity : $response")
        return response
    }

    fun getInstitutionsNoen(id : Int, noe : String) : Institusjon {
        if (id < 0 && noe.isNullOrEmpty()) {
            throw IllegalArgumentException("Feil med Response argumenter")
        }
        try {
            val response = rest.restTemplate.exchange(rest.createGet("$path/getnoe/$id|$noe"), rest.typeRef<Institusjon>())

            if (HttpStatus.OK == response.statusCode) {
                return response.body!!
            } else {
                throw RequestException("Error $this::class.java Noe feil i response")
            }

        } catch(ex : Exception) {
            throw RequestException("Error $this::class.java Noe feil")
        }
    }


    @TestOnly
    fun getTestAllInstitutions() : ResponseEntity<List<Institusjon>>  {
        val list = Lists.newArrayList(Institusjon("SE","Sverige"), Institusjon("DK","Danmark"),Institusjon("FI","Finland"))
        val responseEntity : ResponseEntity<List<Institusjon>> = ResponseEntity(list, HttpStatus.OK)
        logger.debug("ResponseEntity : $responseEntity")
        return responseEntity
    }

    @TestOnly
    fun getTestInstitutionByID(id : String) : ResponseEntity<Institusjon> {
        val data  = Institusjon("SE","Sverige")
        val responseEntity : ResponseEntity<Institusjon> = ResponseEntity(data, HttpStatus.OK)
        logger.debug("ResponseEntity : $responseEntity")
        return responseEntity
    }

}