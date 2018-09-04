package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.clients.personv3.PersonV3Client
import no.nav.eessi.eessifagmodul.models.RINAaksjoner
import no.nav.eessi.eessifagmodul.services.AktoerregisterService
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.security.oidc.api.Protected
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/experiments")
@Protected
class ExperimentController {

    @Autowired
    private lateinit var personV3Client: PersonV3Client

    @Autowired
    private lateinit var euxService: EuxService

    @Autowired
    private lateinit var aktoerregisterService: AktoerregisterService

    @GetMapping("/testAktoer/{ident}")
    fun testAktoer(@PathVariable("ident") ident: String): String {
        return aktoerregisterService.hentGjeldendeAktorIdForNorskIdent(ident)
    }

    @GetMapping("/testAktoerTilIdent/{ident}")
    fun testAktoerTilIdent(@PathVariable("ident") ident: String): String {
        //return aktoerIdClient.hentIdentForAktoerId(ident)?.ident
        return aktoerregisterService.hentGjeldendeNorskIdentForAktorId(ident)
    }

    @GetMapping("/testPerson/{ident}")
    fun testPerson(@PathVariable("ident") ident: String): HentPersonResponse {
        return personV3Client.hentPerson(ident)
    }

    @GetMapping("/possibleactions/{rinanr}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getMuligeAksjoner(@PathVariable(value = "rinanr",  required = true)rinanr: String): List<RINAaksjoner> {
        return euxService.getPossibleActions(rinanr)
    }
}

