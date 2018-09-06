package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.clients.personv3.PersonV3Client
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.services.AktoerregisterService
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs
import no.nav.security.oidc.api.Protected
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.*

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

    //TODO remove when done!
    private fun mockSED(request: ApiController.ApiRequest) : SED {
        val sed: SED?
        when {
            request.payload == null -> throw IkkeGyldigKallException("Mangler PayLoad")
            request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
            else -> {
                val seds = mapJsonToAny(request.payload, typeRefs<SED>())
                sed = seds
            }
        }
        return sed
        //end Mocking
    }

    //TODO remove when done!! test and send existing sed json to rina
    @PostMapping("/testingsed")
    fun testingDocument(@RequestBody request: ApiController.ApiRequest): String {

        if (!checkNotNull(request.mockSED)) {
            throw IkkeGyldigKallException("Ikke MOCK!")
        }

        val rinanr = request.euxCaseId
        val korrid = UUID.randomUUID()

        //temp for mock sendt on payload..
        //TODO remove when done!

        val data = PrefillDataModel()
        data.penSaksnummer = request.caseId!!
        data.personNr = "12345678901"
        data.aktoerID = request.pinid!!
        data.buc = request.buc!!
        data.institution = request.institutions!!
        data.sed = mockSED(request)

        val sed = data.sed
        val sedAsJson = mapAnyToJson(sed, true)

        return if (rinanr != null) {
            euxService.createSEDonExistingRinaCase(sedAsJson, rinanr, korrid.toString())
            rinanr
        } else {
            val euSaksnr = euxService.createCaseAndDocument(
                    jsonPayload = sedAsJson,
                    fagSaknr = data.penSaksnummer,
                    mottaker = getFirstInstitution(data.getInstitutionsList()),
                    bucType = data.buc,
                    korrelasjonID = korrid.toString()
            )
            print("(rina) caseid:  $euSaksnr")
            euSaksnr
        }
    }

    //muligens midlertidig metode for å sende kun en mottaker til EUX.
    private fun getFirstInstitution(institutions: List<InstitusjonItem>): String {
        institutions.forEach {
            return it.institution ?: throw IkkeGyldigKallException("institujson kan ikke være tom")
        }
        throw IkkeGyldigKallException("Mangler mottaker register (InstitusjonItem)")
    }

}

