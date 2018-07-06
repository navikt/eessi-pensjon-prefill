package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.config.jaxws.client.AktoerIdClient
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.eessi.eessifagmodul.utils.createListOfSED
import no.nav.eessi.eessifagmodul.utils.createListOfSEDOnBUC
import no.nav.freg.security.oidc.common.OidcTokenAuthentication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/experiments")
class ExperimentController {

    val objectMapper = jacksonObjectMapper()

    @Autowired
    lateinit var aktoerIdClient: AktoerIdClient

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Value("\${eessibasis.url}")
    lateinit var eessiBasisUrl: String

    @Autowired
    lateinit var euxService: EuxService

    @GetMapping("/testEuxOidc")
    fun testEuxOidc(): ResponseEntity<String> {
        val httpHeaders = HttpHeaders()
        httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer ")
        val requestEntity = RequestEntity<String>(httpHeaders, HttpMethod.GET, URI("$eessiBasisUrl/sample"))

        try {
            return restTemplate.exchange(requestEntity, String::class.java)
        } catch (ex: Exception) {
            ex.printStackTrace()
            println("message: ${ex.message}")
            throw ex
        }
    }

    @GetMapping("/testAktoer/{ident}") //, authentication: OAuth2AuthenticationToken
    fun testAktoer(@PathVariable("ident") ident: String): String? {
        val auth = SecurityContextHolder.getContext().authentication as OidcTokenAuthentication
//        val oidcUser = authentication.principal as OidcUser
        return aktoerIdClient.hentAktoerIdForIdent(ident, auth.idToken)?.aktoerId
    }


    @GetMapping("/opprett")
    fun createCaseAndDocument(): String? {
        val fagSaknr = "SAK-123456"
        val mottaker = "NO"
        val pensjon = genererMockData()
        val pensjonAsJson = objectMapper.writeValueAsString(pensjon)
        val bucType = "P6000"
        val korrid = UUID.randomUUID()
        val vedleggType = ""

        try {
            val data = euxService.createCaseAndDocument(pensjonAsJson, bucType, fagSaknr, mottaker, vedleggType, korrid.toString())
            println("Response: $data")
            println("Skal komme hit!!")
            return data
        } catch (ex: Exception) {
            println("Skal _IKKE_ komme hit!!")
            throw RuntimeException(ex.message)
        }
    }

    @ApiOperation("henter liste av alle BuC og tilhørende SED med forklaring")
    @GetMapping("/detailbucs", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getDetailBucs(): List<BUC> {
        return createPensjonBucList()
    }
    @GetMapping("/detailseds", "/detailseds/{buc}")
    fun getDetailSeds(@PathVariable(value = "buc", required = false) buc: String?): List<SED> {
        if (buc == null) {
            return createListOfSED()
        }
        return createListOfSEDOnBUC(BUC(bucType = buc))
    }

    @GetMapping("/possibleactions/{rinanr}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getMuligeAksjoner(@PathVariable(value = "rinanr",  required = true)rinanr: String): String {
        return euxService.getMuligeAksjoner(rinanr)
    }

    @ApiOperation("Søk med BuCType etter eksisterende RINA saker fra EUX")
    @GetMapping("/rinacase/buc/{bucType}")
    fun getRinaSakerBucType(@PathVariable(value = "bucType",  required = false) bucType: String = ""): List<RINASaker> {
        return euxService.getRinaSaker(bucType)
    }

    @ApiOperation("Søk med RinaSaknr etter eksisterende RINA saker fra EUX")
    @GetMapping("/rinacase/rina/{rinanr}")
    fun getRinaSakerCaseID(@PathVariable(value = "rinanr", required = false) rinaNr: String = ""): List<RINASaker> {
        return euxService.getRinaSaker("",rinaNr)
    }
}

fun genererMockData(): Pensjon {
    return PensjonMock().genererMockData()
}