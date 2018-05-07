package no.nav.eessi.eessifagmodul.services

import com.google.common.collect.Lists
import no.nav.eessi.eessifagmodul.models.Institusjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI

@Service
class InstitutionService {

    private val logger: Logger = LoggerFactory.getLogger(InstitutionService::class.java)

    //BASIS urlpath
    val path : String = "/cpi"

    @Autowired
    lateinit var restTemplate: RestTemplate

    init {
        logger.debug("Starter: InstitutionService")
    }

    fun getInstitutionByID(id : String) : Institusjon? {
        val response = restTemplate.getForObject("/insitusion", Institusjon::class.java)
        logger.debug ("Response : $response")
        return response
    }

    fun getAllInstitutions() : List<Institusjon>?  {
        val type : List<Institusjon> = Lists.newArrayList()
        val response = restTemplate.getForObject("$path/getInstitutions", type::class.java)
        logger.debug("Reponse : $response")

        return response

    }




}