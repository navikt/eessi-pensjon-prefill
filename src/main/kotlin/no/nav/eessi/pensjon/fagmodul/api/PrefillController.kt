package no.nav.eessi.pensjon.fagmodul.api

import io.swagger.annotations.ApiOperation
import no.nav.eessi.pensjon.fagmodul.prefill.ApiRequest
import no.nav.eessi.pensjon.fagmodul.prefill.InnhentingService
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillService
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
class PrefillController(
    private val innhentingService: InnhentingService,
    private val prefillService: PrefillService,
    private val auditlogger: AuditLogger) {

    private val logger = LoggerFactory.getLogger(PrefillController::class.java)


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
        return prefillService.prefillSedtoJson(dataModel, "4.2", personcollection)
    }
}
