package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.clients.aktoerid.AktoerIdClient
import no.nav.eessi.eessifagmodul.clients.personv3.PersonV3Client
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.services.RinaActions
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.eessi.eessifagmodul.services.PostnummerService
import no.nav.security.oidc.api.Protected
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
@Protected
class ExperimentController {

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
    private lateinit var muligeAksjoner: RinaActions

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
        return personV3Client.hentPerson(ident)
    }

    @GetMapping("/possibleactions/{rinanr}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getMuligeAksjoner(@PathVariable(value = "rinanr",  required = true)rinanr: String): List<RINAaksjoner> {
        return euxService.getPossibleActions(rinanr)
    }

//    @GetMapping("/aksjoner/{rina}/{sed}/{navn}")
//    fun getAksjoner(@PathVariable("rina", required = true) rinanr: String = "",
//            @PathVariable("sed", required = true) sed: String = "",
//        @PathVariable("navn", required = true) navn: String = "Update"): Boolean {
//        if (navn == "Update") {
//            return rinaActions.canUpdate(sed, rinanr)
//        }
//        return rinaActions.canCreate(sed, rinanr)
//    }

}

