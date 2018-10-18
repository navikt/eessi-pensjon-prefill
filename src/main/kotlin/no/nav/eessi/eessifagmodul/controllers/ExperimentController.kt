package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.models.IkkeGyldigKallException
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.RINAaksjoner
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.services.aktoerregister.AktoerregisterService
import no.nav.eessi.eessifagmodul.services.bucbucket.BucBucketService
import no.nav.eessi.eessifagmodul.services.bucbucket.QueryParameters
import no.nav.eessi.eessifagmodul.services.bucbucket.QueryResult
import no.nav.eessi.eessifagmodul.services.eux.EuxService
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.eessifagmodul.services.personv3.PersonV3Service
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
    private lateinit var pensjonsinformasjonService: PensjonsinformasjonService

    @Autowired
    private lateinit var personV3Service: PersonV3Service

    @Autowired
    private lateinit var euxService: EuxService

    @Autowired
    private lateinit var aktoerregisterService: AktoerregisterService

    @Autowired
    private lateinit var bucBucketService: BucBucketService

    @GetMapping("/testBucBucket/query")
    fun testBucBucketQuery(
            @RequestParam("correlationId", required = false) correlationId: String?,
            @RequestParam("sedId", required = false) sedId: String?,
            @RequestParam("sedType", required = false) sedType: String?,
            @RequestParam("bucType", required = false) bucType: String?,
            @RequestParam("aktoerId", required = false) aktoerId: String?,
            @RequestParam("pin", required = false) pin: String?,
            @RequestParam("navCaseId", required = false) navCaseId: String?,
            @RequestParam("rinaCaseId", required = false) rinaCaseId: String?,
            @RequestParam("journalId", required = false) journalId: String?,
            @RequestParam("tema", required = false) tema: String?,
            @RequestParam("nationCode", required = false) nationCode: String?,
            @RequestParam("maxResults", required = false) maxResults: String?,
            @RequestParam("resultStart", required = false) resultStart: String?
    ): List<QueryResult> {

        val queryParams = mutableMapOf<QueryParameters, String>()
        correlationId?.let { queryParams[QueryParameters.CORRELATION_ID] = correlationId }
        sedId?.let { queryParams[QueryParameters.SED_ID] = sedId }
        sedType?.let { queryParams[QueryParameters.SED_TYPE] = sedType }
        bucType?.let { queryParams[QueryParameters.BUC_TYPE] = bucType }
        aktoerId?.let { queryParams[QueryParameters.AKTOER_ID] = aktoerId }
        pin?.let { queryParams[QueryParameters.PIN] = pin }
        navCaseId?.let { queryParams[QueryParameters.NAV_CASE_ID] = navCaseId }
        rinaCaseId?.let { queryParams[QueryParameters.RINA_CASE_ID] = rinaCaseId }
        journalId?.let { queryParams[QueryParameters.JOURNALPOST_ID] = journalId }
        nationCode?.let { queryParams[QueryParameters.NATION_CODE] = nationCode }
        maxResults?.let { queryParams[QueryParameters.MAX_RESULTS] = maxResults }
        resultStart?.let { queryParams[QueryParameters.RESULT_START] = resultStart }

        return bucBucketService.queryDocuments(queryParams)
    }

    @GetMapping("/testBucBucket/getDocument/{correlationId}")
    fun testBucBucketFetchDocument(@PathVariable("correlationId") correlationId: String): SED {
        return bucBucketService.getDocument(correlationId)
    }

    @GetMapping("/testPensjonsinformasjon/{vedtaksId}")
    fun testPensjonsinformasjon(@PathVariable("vedtaksId") vedtaksId: String): String {
        val response = pensjonsinformasjonService.hentAlt(vedtaksId)
        return response.toString()
    }

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
        return personV3Service.hentPerson(ident)
    }

    @GetMapping("/possibleactions/{rinanr}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getMuligeAksjoner(@PathVariable(value = "rinanr", required = true) rinanr: String): List<RINAaksjoner> {
        return euxService.getPossibleActions(rinanr)
    }

    //TODO remove when done!
    private fun mockSED(request: ApiController.ApiRequest): SED {
        val sed: SED?
        when {
            request.payload == null -> throw IkkeGyldigKallException("Mangler PayLoad")
            request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
            else -> {
                val seds = SED().fromJson(request.payload)
                sed = seds
            }
        }
        return sed
    }

    //TODO remove when done!! test and send existing sed json to rina
    @PostMapping("/testingsed")
    fun testingDocument(@RequestBody request: ApiController.ApiRequest): String {
        //TODO remove when done!
        if (!checkNotNull(request.mockSED)) {
            throw IkkeGyldigKallException("Ikke MOCK!")
        }
        val korrid = UUID.randomUUID()
        val penSaksnr = if (request.caseId == null) { throw IkkeGyldigKallException("Mangler pensjonSaksnr") } else { request.caseId }
        val sedObj = mockSED(request)

        return if (request.euxCaseId != null) {
            val data = PrefillDataModel().apply {
                penSaksnummer = penSaksnr
                //personNr = "12345678901"
                //aktoerID = "12345678901"
                sed = sedObj
                euxCaseID = request.euxCaseId
            }
            euxService.createSEDonExistingRinaCase(data.sed, data.euxCaseID, korrid.toString())
            data.euxCaseID

        } else {
            val bucId = if (request.buc == null) { throw IkkeGyldigKallException("Mangler BUC") } else { request.buc }
            val institutin = if (request.institutions == null) { throw IkkeGyldigKallException("Mangler pensjonSaksnr") } else { request.institutions }

            val data = PrefillDataModel().apply {
                penSaksnummer = penSaksnr
                //personNr = "12345678901"
                //aktoerID = "12345678901"
                buc = bucId
                institution = institutin
                sed = sedObj
            }
            val euSaksnr = euxService.createCaseAndDocument(
                    sed = data.sed,
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

