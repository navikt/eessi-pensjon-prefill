package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.services.aktoerregister.AktoerregisterService
import no.nav.eessi.eessifagmodul.services.bucbucket.BucBucketService
import no.nav.eessi.eessifagmodul.services.bucbucket.QueryParameters
import no.nav.eessi.eessifagmodul.services.bucbucket.QueryResult
import no.nav.eessi.eessifagmodul.services.eux.EuxService
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.eessifagmodul.services.personv3.PersonV3Service
import no.nav.eessi.eessifagmodul.utils.NavFodselsnummer
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
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
        val response = pensjonsinformasjonService.hentAltPaaVedtak(vedtaksId)
        return mapAnyToJson(response)
    }

    @GetMapping("/testPensjonsinformasjonkrav/{fnr}")
    fun testPensjonsinformasjonSakFnr(@PathVariable("fnr") fnr: String): Pensjonsinformasjon {
        return pensjonsinformasjonService.hentAltPaaFnr(fnr)
    }

    @GetMapping("/testPensjonsinformasjonkrav/{fnr}/{sakid}")
    fun testPensjonsinformasjonSakFnrOgSak(@PathVariable("fnr") fnr: String, @PathVariable("sakid") sakId: String): V1Sak {
        return pensjonsinformasjonService.hentAltPaaSak(sakId, pensjonsinformasjonService.hentAltPaaFnr(fnr))
    }

    @GetMapping("/testPensjonPerson/{fnr}/{sakId}")
    fun testPensjonPersonInfo(@PathVariable("fnr") fnrId: String, @PathVariable("sakId") sakId: String): PersonDetail {
        val vSak = pensjonsinformasjonService.hentAltPaaSak(sakId, pensjonsinformasjonService.hentAltPaaFnr(fnrId))

        val sakType = vSak.sakType

        val bucId: String
        bucId = when (sakType) {
            "ALDER" -> "P_BUC_01"
            "GJENLEV" -> "P_BUC_02"
            "UFOREP" -> "P_BUC_03"
            else -> "UKJENT"
        }

        val barnList = vSak.brukersBarnListe
        val aktoerId = aktoerregisterService.hentGjeldendeAktorIdForNorskIdent(fnrId)
        val personv3 = personV3Service.hentPerson(fnrId)
        val kjoenn = personv3.person.kjoenn.kjoenn.value
        val personNavn = personv3.person.personnavn.sammensattNavn

        val sivilStand = personv3.person.sivilstand.sivilstand.value
        val personStatus = personv3.person.personstatus.personstatus.value


        val navfnr = NavFodselsnummer(fnrId)


        return PersonDetail(
                sakType = sakType,
                buc = bucId,
                aktoerId = aktoerId,
                fnr = fnrId,
                personNavn = personNavn,
                kjoenn = kjoenn,
                fodselDato = navfnr.getBirthDate(),
                aar16Dato = navfnr.getYearWhen16(),
                alder = navfnr.getAge(),
                sivilStand = sivilStand,
                persomStatus = personStatus,
                euxCaseId = null
        )
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

    @GetMapping("/institusjoner", "/institusjoner/{land}")
    fun getEuxInstitusjoner(@PathVariable("land", required = false) landkode: String? = ""): List<String> {
        return euxService.getInstitutions(landkode).sorted()
    }

    //TODO remove when done!
    private fun mockSED(request: ApiController.ApiRequest): SED {
        val sed: SED?
        when {
            request.payload == null -> throw IkkeGyldigKallException("Mangler PayLoad")
            request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
            else -> {
                val seds = SED.fromJson(request.payload)
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
        val penSaksnr = request.sakId
        val sedObj = mockSED(request)

        return if (request.euxCaseId != null) {
            val data = PrefillDataModel().apply {
                penSaksnummer = penSaksnr
                sed = sedObj
                euxCaseID = request.euxCaseId
            }
            euxService.createSEDonExistingRinaCase(data.sed, data.euxCaseID, korrid.toString())
            data.euxCaseID

        } else {
            val bucId = request.buc ?: throw IkkeGyldigKallException("Mangler BUC")
            val institutin = request.institutions ?: throw IkkeGyldigKallException("Mangler pensjonSaksnr")

            val data = PrefillDataModel().apply {
                penSaksnummer = penSaksnr
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

