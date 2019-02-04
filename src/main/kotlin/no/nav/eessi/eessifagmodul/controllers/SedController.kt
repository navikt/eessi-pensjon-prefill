package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.services.PrefillService
import no.nav.eessi.eessifagmodul.services.aktoerregister.AktoerregisterService
import no.nav.eessi.eessifagmodul.services.eux.BucSedResponse
import no.nav.eessi.eessifagmodul.services.eux.EuxService
import no.nav.eessi.eessifagmodul.services.eux.bucmodel.DocumentsItem
import no.nav.security.oidc.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

private val logger = LoggerFactory.getLogger(SedController::class.java)

@Protected
@RestController
@RequestMapping("/sed")
class SedController(private val euxService: EuxService,
                    private val prefillService: PrefillService,
                    private val aktoerregisterService: AktoerregisterService) {


    @ApiOperation("Genereren en Nav-Sed (SED), viser en oppsumering av SED. Før evt. innsending til EUX/Rina")
    @PostMapping("/confirm", "/preview", consumes = ["application/json"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    fun confirmDocument(@RequestBody request: ApiRequest): SED {
        return prefillService.prefillSed(buildPrefillDataModelConfirm(request)).sed
    }

    @ApiOperation("Sender valgt sedtype på valgt bucid, ny api kall til eux")
    @PostMapping("/send")
    fun sendSed(@RequestBody request: ApiRequest): Boolean {
        val euxCaseId = request.euxCaseId ?: throw IkkeGyldigKallException("Mangler euxCaseID (RINANR)")
        val sed = request.sed ?: throw IkkeGyldigKallException("Mangler SED")

        val sedType = SEDType.valueOf(sed)
        val documentid = euxService.getBucUtils(euxCaseId).findFirstDocumentItemByType(sedType)?.id
                ?: throw SedDokumentIkkeGyldigException("Fant ikke documentId")

        return euxService.sendDocumentById(euxCaseId, documentid)

    }

    @ApiOperation("henter ut en SED fra et eksisterende Rina document. krever unik dokumentid fra valgt SED, ny api kall til eux")
    @GetMapping("/{rinanr}/{documentid}")
    fun getDocument(@PathVariable("rinanr", required = true) rinaSakId: String,
                    @PathVariable("documentid", required = true) documentid: String): SED {

        return euxService.getSedOnBucByDocumentId(rinaSakId, documentid)

    }

    @ApiOperation("henter ut en liste av SED fra en valgt buc, men bruk av sedType. ny api kall til eux")
    @GetMapping("/{rinanr}/{sedtype}")
    fun getDocument(@PathVariable("rinanr", required = true) rinaSakId: String,
                    @PathVariable("sedtype", required = true) sedType: SEDType): List<SED> {

        return euxService.getSedOnBuc(rinaSakId, sedType)

    }


    @ApiOperation("sletter SED fra et eksisterende Rina document. krever unik dokumentid fra valgt SED, ny api kall til eux")
    @DeleteMapping("/{rinanr}/{documentid}")
    fun deleteDocument(@PathVariable("rinanr", required = true) rinanr: String,
                       @PathVariable("documentid", required = true) sed: String,
                       @PathVariable("documentid", required = true) documentid: String) {

        euxService.deleteDocumentById(rinanr, documentid)

    }


    @ApiOperation("legge til SED på et eksisterende Rina document. kjører preutfylling, ny api kall til eux")
    @PostMapping("/add")
    fun addDocument(@RequestBody request: ApiRequest): BucSedResponse {

        return prefillService.prefillAndAddSedOnExistingCase(buildPrefillDataModelOnExisting(request))

    }

    @ApiOperation("Kjører prosess OpprettBuCogSED på EUX for å få opprette et RINA dokument med en SED, ny api kall til eux")
    @PostMapping("/buc/create")
    fun createDocument(@RequestBody request: ApiRequest): BucSedResponse {

        return prefillService.prefillAndCreateSedOnNewCase(buildPrefillDataModelOnNew(request))

    }

    @ApiOperation("Henter ut en liste av documents på valgt buc. ny api kall til eux")
    @GetMapping("/buc/{rinanr}/documents")
    fun getDocumentId(@PathVariable("rinanr", required = true) rinanr: String): List<DocumentsItem> {

        return euxService.getBucUtils(rinanr).getDocuments()

    }

    //validatate request and convert to PrefillDataModel
    fun buildPrefillDataModelOnExisting(request: ApiRequest): PrefillDataModel {
        return when {
            //request.sakId == null -> throw IkkeGyldigKallException("Mangler Saksnummer")
            request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
            request.aktoerId == null -> throw IkkeGyldigKallException("Mangler AktoerID")
            request.euxCaseId == null -> throw IkkeGyldigKallException("Mangler euxCaseId (RINANR)")

            SEDType.isValidSEDType(request.sed) -> {
                println("ALL SED on existin Rina -> SED: ${request.sed} -> euxCaseId: ${request.sakId}")
                val pinid = hentAktoerIdPin(request.aktoerId)
                PrefillDataModel().apply {
                    penSaksnummer = request.sakId
                    sed = SED.create(request.sed)
                    aktoerID = request.aktoerId
                    personNr = pinid
                    euxCaseID = request.euxCaseId

                    vedtakId = request.vedtakId ?: ""
                    partSedAsJson[request.sed] = request.payload ?: ""
                }
            }
            else -> throw IkkeGyldigKallException("Mangler SED, eller ugyldig type SED")
        }
    }

    //validatate request and convert to PrefillDataModel
    fun buildPrefillDataModelOnNew(request: ApiRequest): PrefillDataModel {
        return when {
            //request.sakId == null -> throw IkkeGyldigKallException("Mangler Saksnummer")
            request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
            request.aktoerId == null -> throw IkkeGyldigKallException("Mangler AktoerID")
            request.buc == null -> throw IkkeGyldigKallException("Mangler BUC")
            request.subjectArea == null -> throw IkkeGyldigKallException("Mangler Subjekt/Sektor")
            request.institutions == null -> throw IkkeGyldigKallException("Mangler Institusjoner")

            //Denne validering og utfylling kan benyttes på SED P2000,P2100,P2200
            SEDType.isValidSEDType(request.sed) -> {
                println("ALL SED on new RinaCase -> SED: ${request.sed}")
                val pinid = hentAktoerIdPin(request.aktoerId)
                PrefillDataModel().apply {
                    penSaksnummer = request.sakId
                    buc = request.buc
                    rinaSubject = request.subjectArea
                    sed = SED.create(request.sed)
                    aktoerID = request.aktoerId
                    personNr = pinid
                    institution = request.institutions
                    vedtakId = request.vedtakId ?: ""
                }
            }
            else -> throw IkkeGyldigKallException("Mangler SED, eller ugyldig type SED")
        }
    }

    //validatate request and convert to PrefillDataModel
    fun buildPrefillDataModelConfirm(request: ApiRequest): PrefillDataModel {
        return when {
            //request.sakId == null -> throw IkkeGyldigKallException("Mangler Saksnummer")
            request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
            request.aktoerId == null -> throw IkkeGyldigKallException("Mangler AktoerID")

            SEDType.isValidSEDType(request.sed) -> {
                PrefillDataModel().apply {
                    penSaksnummer = request.sakId
                    sed = SED.create(request.sed)
                    aktoerID = request.aktoerId
                    personNr = hentAktoerIdPin(request.aktoerId)
                    vedtakId = request.vedtakId ?: ""
                    if (request.payload != null) {
                        partSedAsJson[request.sed] = request.payload
                    }
                }
            }
            else -> throw IkkeGyldigKallException("Mangler SED, eller ugyldig type SED")
        }
    }

    @Throws(AktoerregisterException::class)
    fun hentAktoerIdPin(aktorid: String): String {
        if (aktorid.isBlank()) throw IkkeGyldigKallException("Mangler AktorId")
        return aktoerregisterService.hentGjeldendeNorskIdentForAktorId(aktorid)
    }

    //Samme som SedRequest i frontend-api
    data class ApiRequest(
            val sakId: String,
            val vedtakId: String? = null,
            val kravId: String? = null,
            val aktoerId: String? = null,
            val fnr: String? = null,
            val payload: String? = null,
            val buc: String? = null,
            val sed: String? = null,
            val euxCaseId: String? = null,
            val institutions: List<InstitusjonItem>? = null,
            val subjectArea: String? = null,
            val mockSED: Boolean? = null
    )
}