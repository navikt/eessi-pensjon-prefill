package no.nav.eessi.pensjon.fagmodul.api

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.swagger.annotations.ApiOperation
import no.nav.eessi.pensjon.fagmodul.prefill.ApiRequest
import no.nav.eessi.pensjon.fagmodul.prefill.InnhentingService
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillService
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct

@Protected
@RestController
class PrefillController(
    private val innhentingService: InnhentingService,
    private val prefillService: PrefillService,
    private val auditlogger: AuditLogger,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())
) {

    private val logger = LoggerFactory.getLogger(PrefillController::class.java)

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
    @PostMapping("sed/prefill", consumes = ["application/json"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun prefillDocument(
        @RequestBody request: ApiRequest,
        @PathVariable("filter", required = false) filter: String? = null
    ): String {
        auditlogger.log("previewDocument", request.aktoerId ?: "", request.toAudit())
        logger.info("Prefiller : ${request.sed}")
        val norskIdent = innhentingService.hentFnrfraAktoerService(request.aktoerId)
        val dataModel = ApiRequest.buildPrefillDataModelOnExisting(request, norskIdent, innhentingService.getAvdodAktoerIdPDL(request))

        val personcollection = innhentingService.hentPersonData(dataModel)
        return prefillService.prefillSedtoJson(dataModel, "4.2", personcollection).sed
    }
}
