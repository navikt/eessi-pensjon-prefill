package no.nav.eessi.pensjon.fagmodul.api

import io.swagger.annotations.ApiOperation
import no.nav.eessi.pensjon.fagmodul.eux.BucUtils
import no.nav.eessi.pensjon.fagmodul.eux.EuxService
import no.nav.eessi.pensjon.fagmodul.eux.PinOgKrav
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.BucSedResponse
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.ShortDocumentItem
import no.nav.eessi.pensjon.fagmodul.prefill.ApiRequest
import no.nav.eessi.pensjon.fagmodul.prefill.MangelfulleInndataException
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillService
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.helper.AktoerIdHelper
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import no.nav.security.oidc.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@Protected
@RestController
@RequestMapping("/sed")
class SedController(private val euxService: EuxService,
                    private val prefillService: PrefillService,
                    private val aktoerIdHelper: AktoerIdHelper,
                    private val auditlogger: AuditLogger) {

    private val logger = LoggerFactory.getLogger(SedController::class.java)


    //** oppdatert i api 18.02.2019
    @ApiOperation("Genereren en Nav-Sed (SED), viser en oppsumering av SED (json). Før evt. innsending til EUX/Rina")
    @PostMapping("/preview", "/preview/{filter}", consumes = ["application/json"], produces = [org.springframework.http.MediaType.APPLICATION_JSON_VALUE])
    fun confirmDocument(@RequestBody request: ApiRequest, @PathVariable("filter", required = false) filter: String ?= null): String {
        val dataModel = ApiRequest.buildPrefillDataModelConfirm(request, aktoerIdHelper.hentPinForAktoer(request.aktoerId), getAvdodAktoerId(request))
        auditlogger.log("confirmDocument", request.aktoerId ?: "" , request.toAudit())
        val sed = prefillService.prefillSed(dataModel).sed
        return if (filter==null) {
            sed.toJsonSkipEmpty()
        } else {
            sed.toJson()
        }

    }



    //** oppdatert i api 18.02.2019
    @ApiOperation("Sender valgt NavSed på rina med valgt documentid og bucid, ut til eu/eøs, ny api kall til eux")
    @GetMapping("/send/{euxcaseid}/{documentid}")
    fun sendSed(@PathVariable("euxcaseid", required = true) euxCaseId: String,
                @PathVariable("documentid", required = true) documentid: String): Boolean {
        auditlogger.logBuc("sendSed", " euxCaseId: $euxCaseId documentId: $documentid")
        logger.info("kaller /cpi/buc/$euxCaseId/sed/$documentid/send")
        return euxService.sendDocumentById(euxCaseId, documentid)
    }

    //** oppdatert i api 18.02.2019
    @ApiOperation("henter ut en SED fra et eksisterende Rina document. krever unik dokumentid fra valgt SED, ny api kall til eux")
    @GetMapping("/{euxcaseid}/{documentid}")
    fun getDocument(@PathVariable("euxcaseid", required = true) euxcaseid: String,
                    @PathVariable("documentid", required = true) documentid: String): SED {
        auditlogger.logBuc("getDocument", " euxCaseId: $euxcaseid documentId: $documentid")

        logger.info("kaller /${euxcaseid}/${documentid} ")
        return euxService.getSedOnBucByDocumentId(euxcaseid, documentid)

    }

    //** oppdatert i api 18.02.2019
    @ApiOperation("sletter SED fra et eksisterende Rina document. krever unik dokumentid fra valgt SED, ny api kall til eux")
    @DeleteMapping("/{euxcaseid}/{documentid}")
    fun deleteDocument(@PathVariable("euxcaseid", required = true) euxcaseid: String,
                       @PathVariable("documentid", required = true) documentid: String): Boolean {
        auditlogger.logBuc("deleteDocument", " euxCaseId: $euxcaseid documentId: $documentid")
        logger.info("kaller delete  /${euxcaseid}/${documentid} ")
        return euxService.deleteDocumentById(euxcaseid, documentid)

    }

    //** oppdatert i api 18.02.2019
    @ApiOperation("legge til Deltaker(e) og SED på et eksisterende Rina document. kjører preutfylling, ny api kall til eux")
    @PostMapping("/add")
    fun addInstutionAndDocument(@RequestBody request: ApiRequest): ShortDocumentItem {
        auditlogger.log("addInstutionAndDocument", request.aktoerId ?: "", request.toAudit())
        val dataModel = ApiRequest.buildPrefillDataModelOnExisting(request, aktoerIdHelper.hentPinForAktoer(request.aktoerId), getAvdodAktoerId(request))

        logger.info("kaller add (institutions and sed)")
        logger.debug("Prøver å legge til Deltaker/Institusions på buc samt prefillSed og sende inn til Rina ")
        val bucUtil = BucUtils(euxService.getBuc(dataModel.euxCaseID))
        val nyeDeltakere = bucUtil.findNewParticipants(dataModel.getInstitutionsList())
        if (nyeDeltakere.isNotEmpty()) {
            logger.debug("DeltakerListe (InstitusjonItem) size: ${nyeDeltakere.size}")
            val bucX005 = bucUtil.findFirstDocumentItemByType("X005")
            if (bucX005 == null) {
                logger.debug("X005 finnes ikke på buc, legger til Deltakere/Institusjon på vanlig måte")
                //kaste Exception dersom legge til deltakerfeiler?
                euxService.putBucMottakere(dataModel.euxCaseID, nyeDeltakere)
            } else {
                logger.debug("X005 finnes på buc, Sed X005 prefills og sendes inn")
                val x005Liste = prefillService.prefillEnX005ForHverInstitusjon(nyeDeltakere, dataModel)
                x005Liste.forEach { x005 -> euxService.opprettSedOnBuc(x005.sed, x005.euxCaseID) }
            }
        }
        val data = prefillService.prefillSed(dataModel)
        logger.debug("Prøver å sende SED:${dataModel.getSEDid()} inn på buc: ${dataModel.euxCaseID}")
        val docresult = euxService.opprettSedOnBuc(data.sed, data.euxCaseID)
        return BucUtils(euxService.getBuc(docresult.caseId)).findDocument(docresult.documentId)

    }

    @ApiOperation("Oppretter en Sed som svar på en forespørsel-Sed")
    @RequestMapping("/replysed/{parentid}", method = [ RequestMethod.POST ])
    fun addDocumentToParent(@RequestBody(required = true) request: ApiRequest, @PathVariable("parentid", required = true) parentId: String  ): ShortDocumentItem {
        auditlogger.log("addDocumentToParent", request.aktoerId ?: "", request.toAudit())

        val dataModel = ApiRequest.buildPrefillDataModelOnExisting(request, aktoerIdHelper.hentPinForAktoer(request.aktoerId),  getAvdodAktoerId(request))
        val data = prefillService.prefillSed(dataModel)

        logger.debug("Prøver å sende SED:${dataModel.getSEDid()} inn på buc: ${dataModel.euxCaseID}")
        val docresult = euxService.opprettSvarSedOnBuc(data.sed, data.euxCaseID, parentId)
        return BucUtils(euxService.getBuc(docresult.caseId)).findDocument(docresult.documentId)
    }

    //** oppdatert i api 18.02.2019
    @ApiOperation("Utgår?")
    @PostMapping("/addSed")
    fun addDocument(@RequestBody request: ApiRequest): ShortDocumentItem {
        auditlogger.log("addDocument", request.aktoerId ?: "", request.toAudit())
        val dataModel = ApiRequest.buildPrefillDataModelOnExisting(request, aktoerIdHelper.hentPinForAktoer(request.aktoerId),  getAvdodAktoerId(request))
        val data = prefillService.prefillSed(dataModel)

        logger.info("kaller add med request: $request")
        val docresult = euxService.opprettSedOnBuc(data.sed, data.euxCaseID)
        return BucUtils(euxService.getBuc(docresult.caseId)).findDocument(docresult.documentId)
    }

    @ApiOperation("Utgår?")
    @PostMapping("/buc/create")
    fun createDocument(@RequestBody request: ApiRequest): BucSedResponse {
        auditlogger.log("createDocument", request.aktoerId ?: "", request.toAudit())
        logger.info("kaller type/create med request: $request")

        val dataModel = ApiRequest.buildPrefillDataModelOnNew(request, aktoerIdHelper.hentPinForAktoer(request.aktoerId))
        val data = prefillService.prefillSed(dataModel)
        val firstInstitution =
                data.institution.firstOrNull() ?: throw ManglendeInstitusjonException("institusjon kan ikke være tom")
        return euxService.opprettBucSed(data.sed, data.buc, firstInstitution.institution, data.penSaksnummer)
    }

    //** oppdatert i api 18.02.2019 --
    @ApiOperation("Utgår, benytt /buc/rinanr/allDocuments ")
    @GetMapping("/buc/{euxcaseid}/shortdocumentslist")
    fun getShortDocumentList(@PathVariable("euxcaseid", required = true) euxcaseid: String): List<ShortDocumentItem> {
        auditlogger.logBuc("getShortDocumentList", " euxCaseId: $euxcaseid")
        logger.info("kaller /type/${euxcaseid}/documents ")

        return BucUtils(euxService.getBuc(euxcaseid)).getAllDocuments()
    }

    //TODO endre denne til å gå til denne: /cpi/buc/{RinaSakId}/sedtyper  (istede for benytte seg av egen bucutil)
    @ApiOperation("henter ut en liste av SED fra en valgt type, men bruk av sedType. ny api kall til eux")
    @GetMapping("list/{euxcaseid}/{sedtype}")
    fun getDocumentlist(@PathVariable("euxcaseid", required = true) euxcaseid: String,
                        @PathVariable("sedtype", required = false) sedType: String?): List<SED> {
        auditlogger.logBuc("getDocumentlist", " euxCaseId: $euxcaseid")
        logger.info("kaller /${euxcaseid}/${sedType} ")

        return euxService.getSedOnBuc(euxcaseid, sedType)
    }

    @ApiOperation("Henter ut en liste over registrerte institusjoner innenfor spesifiserte EU-land. ny api kall til eux")
    @GetMapping("/institusjoner/{buctype}", "/institusjoner/{buctype}/{land}")
    fun getEuxInstitusjoner(@PathVariable("buctype", required = true) buctype: String, @PathVariable("land", required = false) landkode: String? = ""): List<String> {
        logger.info("Henter ut liste over alle Institusjoner i Rina")

        return euxService.getInstitutions(buctype, landkode).sorted()
    }

    @ApiOperation("henter liste over seds, seds til valgt buc eller seds til valgt rinasak")
    @GetMapping("/seds", "/seds/{buctype}", "/seds/{buctype}/{rinanr}")
    fun getSeds(@PathVariable(value = "buctype", required = false) bucType: String?,
                @PathVariable(value = "rinanr", required = false) euxCaseId: String?): ResponseEntity<String?> {

        //Ingen buc oppgitt, vi lister våre seds på valgt buctype.
        if (euxCaseId == null) return ResponseEntity.ok().body( EuxService.getAvailableSedOnBuc(bucType).toJsonSkipEmpty() )
        //liste over seds som kan opprettes fra Rina på valgt euxCaseid (rinanr)
        val resultListe = BucUtils(euxService.getBuc(euxCaseId)).getAksjonListAsString()
        //hvis tom er stort sett buc helt ny. vi lister våre seds på valgt buctype.
        if (resultListe.isEmpty()) return ResponseEntity.ok().body( EuxService.getAvailableSedOnBuc(bucType).toJsonSkipEmpty() )
        //hvis liste ikke tom vi filterer listen
        return ResponseEntity.ok().body( sortAndFilterSeds(resultListe).toJsonSkipEmpty() )
    }

    //fjerner uønskdede seder fra liste og kun filterer inn kun ønskelige og seder vi støtter
    fun sortAndFilterSeds(list: List<String>): List<String> {
        return list.filter { it.startsWith("P")
                    .or(it.startsWith("H12"))
                    .or(it.startsWith("H07"))
                    .or(it.startsWith("H02")) }
                .filterNot { it.startsWith("P3000") }.sorted()
    }

    @ApiOperation("Henter ytelsetype fra P15000 på valgt Buc og Documentid")
    @GetMapping("/ytelseKravtype/{rinanr}/sedid/{documentid}")
    fun getPinOgYtelseKravtype(@PathVariable("rinanr", required = true) rinanr: String,
                               @PathVariable("documentid", required = false) documentid: String): PinOgKrav {
        auditlogger.logBuc("getPinOgYtelseKravtype", " euxCaseId: $rinanr  documentId: $documentid")
        logger.debug("Henter opp ytelseKravType fra P2100 eller P15000, feiler hvis ikke rett SED")
        return euxService.hentFnrOgYtelseKravtype(rinanr, documentid)
    }

    //Hjelpe funksjon for å validere og hente aktoerid for evt. avdodfnr fra UI (P2100)
    fun getAvdodAktoerId(request: ApiRequest): String? {
        return if ((request.buc ?: throw MangelfulleInndataException("Mangler Buc")) == "P_BUC_02")
            aktoerIdHelper.hentAktoerForPin((request.avdodfnr
                    ?: throw MangelfulleInndataException("Mangler fnr for avdød")))
        else null
    }

    @ApiOperation("Henter fodselsdato fra sed for valgt euxcaseid")
    @GetMapping("/fodselsdato/{rinanr}/buctype/{buctype}")
    fun getFodselsdato(@PathVariable("rinanr", required = true) rinanr: String,
                       @PathVariable("buctype", required = true) buctype: String): String? {
        auditlogger.logBuc("getFodselsdato", " euxCaseId: $rinanr  buctype: $buctype")
        logger.debug("Henter opp fødselsdato fra sed for valgt euxcaseid")
        return euxService.getFDatoFromSed(rinanr, buctype)
    }

    @ApiOperation("Henter fødselsnr/personnr fra første sed for valgt euxcaseid")
    @GetMapping("/fodselsnr/{rinanr}/buctype/{buctype}")
    fun getFodselsnrValgtBuc(@PathVariable("rinanr", required = true) rinanr: String,
                       @PathVariable("buctype", required = true) buctype: String): String? {
        auditlogger.logBuc("getFodselsdato", " euxCaseId: $rinanr  buctype: $buctype")
        logger.debug("Henter opp fødselsdato fra sed for valgt euxcaseid")
        return euxService.getFodselsnrFraSedPaaVagtBuc(rinanr, buctype)
    }

}

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class ManglendeInstitusjonException(message: String) : IllegalArgumentException(message)
