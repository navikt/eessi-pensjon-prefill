package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.clients.aktoerid.AktoerIdClient
import no.nav.eessi.eessifagmodul.clients.personv3.PersonV3Client
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.services.EuxMuligeAksjoner
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.eessi.eessifagmodul.services.PostnummerService
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.net.URI

@CrossOrigin
@RestController
@RequestMapping("/api/experiments")
class ExperimentController {

    private val objectMapper = jacksonObjectMapper()

    @Autowired
    private lateinit var personV3Client: PersonV3Client

    @Autowired
    private lateinit var aktoerIdClient: AktoerIdClient

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Value("\${eessibasis.url}")
    private lateinit var eessiBasisUrl: String

    @Autowired
    private lateinit var euxService: EuxService

    @Autowired
    private lateinit var muligeAksjoner: EuxMuligeAksjoner

    @Autowired
    private lateinit var postnummerService: PostnummerService

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

    @GetMapping("/testAktoer/{ident}")
    fun testAktoer(@PathVariable("ident") ident: String): String? {
        return aktoerIdClient.hentAktoerIdForIdent(ident)?.aktoerId
    }

    @GetMapping("/testAktoerTilIdent/{ident}")
    fun testAktoerTilIdent(@PathVariable("ident") ident: String): String {
        //return aktoerIdClient.hentIdentForAktoerId(ident)?.ident
        return aktoerIdClient.hentPinIdentFraAktorid(ident)
    }

    @GetMapping("/testPerson/{ident}")
    fun testPerson(@PathVariable("ident") ident: String): HentPersonResponse {
        val personV3 = personV3Client.hentPerson(ident)
        return personV3
    }

    @GetMapping("/possibleactions/{rinanr}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getMuligeAksjoner(@PathVariable(value = "rinanr",  required = true)rinanr: String): List<RINAaksjoner> {
        return euxService.getMuligeAksjoner(rinanr)
    }
//
//    @ApiOperation("Søk med BuCType etter eksisterende RINA saker fra EUX")
//    @GetMapping("/rinacase/buc/{bucType}")
//    fun getRinaSakerBucType(@PathVariable(value = "bucType",  required = false) bucType: String = ""): List<RINASaker> {
//        return euxService.getRinaSaker(bucType)
//    }
//
//    @ApiOperation("Søk med RinaSaknr etter eksisterende RINA saker fra EUX")
//    @GetMapping("/rinacase/rina/{rinanr}")
//    fun getRinaSakerCaseID(@PathVariable(value = "rinanr", required = false) rinaNr: String = ""): List<RINASaker> {
//        return euxService.getRinaSaker("",rinaNr)
//    }
//    @GetMapping("/rinacase/pinid/{pinid}")
//    fun getRinaSakerPindID(@PathVariable(value = "pinid", required = true) pinID: String = ""): List<RINASaker> {
//        return euxService.getRinaSaker("", "",pinID)
//    }

    @ApiOperation("Sjekke Aksjoner er mulig")
    @GetMapping("/aksjoner/{rina}/{sed}/{navn}")
    fun getAksjoner(@PathVariable("rina", required = true) rinanr: String = "",
            @PathVariable("sed", required = true) sed: String = "",
        @PathVariable("navn", required = true) navn: String = "Update"): Boolean {
        if (navn == "Update") {
            return muligeAksjoner.confirmUpdate(sed, rinanr)
        }
        return muligeAksjoner.confirmCreate(sed, rinanr)
    }

}

