package no.nav.eessi.pensjon.fagmodul.api

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.swagger.annotations.ApiOperation
import no.nav.eessi.pensjon.fagmodul.eux.BucUtils
import no.nav.eessi.pensjon.fagmodul.eux.EuxInnhentingService
import no.nav.eessi.pensjon.fagmodul.eux.EuxPrefillService
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.BucSedResponse
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.DocumentsItem
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.prefill.*
import no.nav.eessi.pensjon.fagmodul.sedmodel.*
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentType
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.utils.Fodselsnummer
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import javax.annotation.PostConstruct

@Protected
@RestController
@RequestMapping("/sed")
class SedController(
    private val innhentingService: InnhentingService,
    private val euxPrefillService: EuxPrefillService,
    private val euxInnhentingService: EuxInnhentingService,
    private val prefillService: PrefillService,
    private val auditlogger: AuditLogger,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())
) {

    private val logger = LoggerFactory.getLogger(SedController::class.java)

    private lateinit var addInstutionAndDocument: MetricsHelper.Metric
    private lateinit var addDocumentToParent: MetricsHelper.Metric
    private lateinit var addInstutionAndDocumentBucUtils: MetricsHelper.Metric
    private lateinit var addDocumentToParentBucUtils: MetricsHelper.Metric

    @PostConstruct
    fun initMetrics() {
        addInstutionAndDocument =
            metricsHelper.init("AddInstutionAndDocument", ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST))
        addDocumentToParent =
            metricsHelper.init("AddDocumentToParent", ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST))
        addInstutionAndDocumentBucUtils =
            metricsHelper.init("AddInstutionAndDocumentBucUtils", ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST))
        addDocumentToParentBucUtils =
            metricsHelper.init("AddDocumentToParentBucUtils", ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST))
    }

    @ApiOperation("Generer en Nav-Sed (SED), viser en oppsumering av SED (json). Før evt. innsending til EUX/Rina")
    @PostMapping("/prefill", consumes = ["application/json"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun prefillDocument(
        @RequestBody request: ApiRequest,
        @PathVariable("filter", required = false) filter: String? = null
    ): String {
        auditlogger.log("previewDocument", request.aktoerId ?: "", request.toAudit())
        logger.info("Prefiller : ${request.sed}")
        val norskIdent = innhentingService.hentFnrfraAktoerService(request.aktoerId)
        val dataModel = ApiRequest.buildPrefillDataModelOnExisting(request, norskIdent, getAvdodAktoerIdPDL(request))

        val personcollection = innhentingService.hentPersonData(dataModel)
        return prefillService.prefillSedtoJson(dataModel, "4.2", personcollection).sed
    }

    @ApiOperation("Henter ut en SED fra et eksisterende Rina document. krever unik dokumentid fra valgt SED, ny api kall til eux")
    @GetMapping("/get/{euxcaseid}/{documentid}")
    fun getDocument(
        @PathVariable("euxcaseid", required = true) euxcaseid: String,
        @PathVariable("documentid", required = true) documentid: String
    ): String {
        auditlogger.logBuc("getDocument", " euxCaseId: $euxcaseid documentId: $documentid")
        logger.info("Hente SED innhold for /${euxcaseid}/${documentid} ")
        val sed = euxInnhentingService.getSedOnBucByDocumentId(euxcaseid, documentid)
        return mapToConcreteSedJson(sed)
    }

    @ApiOperation("Legge til Deltaker(e) og SED på et eksisterende Rina document. kjører preutfylling, ny api kall til eux")
    @PostMapping("/add")
    fun addInstutionAndDocument(@RequestBody request: ApiRequest): DocumentsItem? {
        auditlogger.log("addInstutionAndDocument", request.aktoerId ?: "", request.toAudit())
        logger.info("Legger til institusjoner og SED for rinaId: ${request.euxCaseId} bucType: ${request.buc} sedType: ${request.sed} aktoerId: ${request.aktoerId} sakId: ${request.sakId} vedtak: ${request.vedtakId}")
        val norskIdent = innhentingService.hentFnrfraAktoerService(request.aktoerId)
        val dataModel = ApiRequest.buildPrefillDataModelOnExisting(request, norskIdent, getAvdodAktoerIdPDL(request))

        //Hente metadata for valgt BUC
        val bucUtil = euxInnhentingService.kanSedOpprettes(dataModel)
        val personData = innhentingService.hentPersonData(dataModel)

        //Preutfyll av SED, pensjon og personer samt oppdatering av versjon
        val sedAndType = prefillService.prefillSedtoJson(
            dataModel,
            bucUtil.getProcessDefinitionVersion(),
            personData
        )

        //Sjekk og opprette deltaker og legge sed på valgt BUC
        return addInstutionAndDocument.measure {
            logger.info("******* Legge til ny SED - start *******")

            val sedType = sedAndType.sedType
            val sedJson = sedAndType.sed

            val nyeInstitusjoner = bucUtil.findNewParticipants(dataModel.getInstitutionsList())
            val x005Liste = prefillService.prefillEnX005ForHverInstitusjon(nyeInstitusjoner, dataModel, personData)

            //sjekk og evt legger til deltakere
            euxPrefillService.checkAndAddInstitution(dataModel, bucUtil, x005Liste)

            logger.info("Prøver å sende SED: ${dataModel.sedType} inn på BUC: ${dataModel.euxCaseID}")
            val docresult = euxPrefillService.opprettJsonSedOnBuc(sedJson, sedType, dataModel.euxCaseID, request.vedtakId)

            logger.info("Opprettet ny SED med dokumentId: ${docresult.documentId}")
            val result = bucUtil.findDocument(docresult.documentId)
            if (dataModel.melding != null || dataModel.melding != "") {
                result?.message = dataModel.melding
            }

            val documentItem = fetchBucAgainBeforeReturnShortDocument(dataModel.buc, docresult, result)
            logger.info("******* Legge til ny SED - slutt *******")
            documentItem
        }

    }

    @ApiOperation("Oppretter en Sed som svar på en forespørsel-Sed")
    @PostMapping("/replysed/{parentid}")
    fun addDocumentToParent(
        @RequestBody(required = true) request: ApiRequest,
        @PathVariable("parentid", required = true) parentId: String
    ): DocumentsItem? {
        auditlogger.log("addDocumentToParent", request.aktoerId ?: "", request.toAudit())
        val norskIdent = innhentingService.hentFnrfraAktoerService(request.aktoerId)
        val dataModel = ApiRequest.buildPrefillDataModelOnExisting(request, norskIdent, getAvdodAktoerIdPDL(request))

        //Hente metadata for valgt BUC
        val bucUtil = addDocumentToParentBucUtils.measure {
            logger.info("******* Hent BUC sjekk om sed kan opprettes *******")
            BucUtils(euxInnhentingService.getBuc(dataModel.euxCaseID)).also { bucUtil ->
                //sjekk for om deltakere alt er fjernet med x007 eller x100 sed
                bucUtil.checkForParticipantsNoLongerActiveFromXSEDAsInstitusjonItem(dataModel.getInstitutionsList())
                //sjekk om en svarsed kan opprettes eller om den alt finnes
                bucUtil.checkIfSedCanBeCreatedEmptyStatus(dataModel.sedType, parentId)
            }
        }

        logger.info("Prøver å prefillSED (svarSED) parentId: $parentId")
        val personcollection = innhentingService.hentPersonData(dataModel)
        val sedAndType =
            prefillService.prefillSedtoJson(dataModel, bucUtil.getProcessDefinitionVersion(), personcollection)

        return addDocumentToParent.measure {
            logger.info("Prøver å sende SED: ${dataModel.sedType} inn på BUC: ${dataModel.euxCaseID}")

            val docresult =
                euxPrefillService.opprettSvarJsonSedOnBuc(sedAndType.sed, dataModel.euxCaseID, parentId, request.vedtakId)

            val parent = bucUtil.findDocument(parentId)
            val result = bucUtil.findDocument(docresult.documentId)

            val documentItem = fetchBucAgainBeforeReturnShortDocument(dataModel.buc, docresult, result)

            logger.info("Buc: (${dataModel.euxCaseID}, hovedSED type: ${parent?.type}, docId: ${parent?.id}, svarSED type: ${documentItem?.type} docID: ${documentItem?.id}")
            logger.info("******* Legge til svarSED - slutt *******")
            documentItem
        }
    }

    @ApiOperation("Henter ut en liste over registrerte institusjoner innenfor spesifiserte EU-land. ny api kall til eux")
    @GetMapping("/institutions/{buctype}", "/institutions/{buctype}/{countrycode}")
    fun getEuxInstitusjoner(
        @PathVariable("buctype", required = true) buctype: String,
        @PathVariable("countrycode", required = false) landkode: String? = ""
    ): List<InstitusjonItem> {
        logger.info("Henter ut liste over alle Institusjoner i Rina")
        return euxInnhentingService.getInstitutions(buctype, landkode)
    }

    @ApiOperation("henter liste over seds som kan opprettes til valgt rinasak")
    @GetMapping("/seds/{buctype}/{rinanr}")
    fun getSeds(
        @PathVariable(value = "buctype", required = true) bucType: String,
        @PathVariable(value = "rinanr", required = true) euxCaseId: String
    ): ResponseEntity<String?> {
        val resultListe = BucUtils(euxInnhentingService.getBuc(euxCaseId)).getFiltrerteGyldigSedAksjonListAsString()
        logger.info("Henter lite over SED som kan opprettes på buctype: $bucType seds: $resultListe")
        return ResponseEntity.ok().body(resultListe.toJsonSkipEmpty())
    }

    //Hjelpe funksjon for å validere og hente aktoerid for evt. avdodfnr fra UI (P2100) - PDL
    fun getAvdodAktoerIdPDL(request: ApiRequest): String? {
        val buc = request.buc ?: throw MangelfulleInndataException("Mangler Buc")
        return when (buc) {
            "P_BUC_02" -> {
                val norskIdent = request.riktigAvdod() ?: run {
                    logger.error("Mangler fnr for avdød")
                    throw MangelfulleInndataException("Mangler fnr for avdød")
                }
                if (norskIdent.isBlank()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ident har tom input-verdi")
                }
                innhentingService.hentIdent(IdentType.AktoerId, NorskIdent(norskIdent))
            }
            "P_BUC_05", "P_BUC_06", "P_BUC_10" -> {
                val norskIdent = request.riktigAvdod() ?: return null
                if (norskIdent.isBlank()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ident har tom input-verdi")
                }

                val gyldigNorskIdent = Fodselsnummer.fra(norskIdent)
                return try {
                    innhentingService.hentIdent(IdentType.AktoerId, NorskIdent(norskIdent))
                } catch (ex: Exception) {
                    if (gyldigNorskIdent == null) logger.error("NorskIdent er ikke gyldig")
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "Korrekt aktoerIdent ikke funnet")
                }
            }
            else -> null
        }
    }

    private fun mapToConcreteSedJson(sedJson: SED): String {
        return when (sedJson.type) {
            SEDType.P4000 -> (sedJson as P4000).toJson()
            SEDType.P5000 -> (sedJson as P5000).toJson()
            SEDType.P6000 -> (sedJson as P6000).toJson()
            SEDType.P7000 -> (sedJson as P7000).toJson()
            SEDType.P8000 -> (sedJson as P8000).toJson()
            else -> sedJson.toJson()
        }
    }

    fun fetchBucAgainBeforeReturnShortDocument(
        bucType: String,
        bucSedResponse: BucSedResponse,
        orginal: DocumentsItem?
    ): DocumentsItem? {
        return if (bucType == "P_BUC_06") {
            logger.info("Henter BUC på nytt for buctype: $bucType")
            val buc = euxInnhentingService.getBuc(bucSedResponse.caseId)
            val bucUtil = BucUtils(buc)
            bucUtil.findDocument(bucSedResponse.documentId)
        } else {
            orginal
        }
    }


}
