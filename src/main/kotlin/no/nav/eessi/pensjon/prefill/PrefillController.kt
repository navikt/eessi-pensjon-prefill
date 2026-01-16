package no.nav.eessi.pensjon.prefill

import jdk.internal.org.jline.utils.Colors.s
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
@Protected
@RestController
class PrefillController(private val prefillService: PrefillService, private val prefillGjennyService: PrefillGjennyService, private val auditlogger: AuditLogger) {

    private val logger = LoggerFactory.getLogger(PrefillController::class.java)
    @PostMapping("sed/prefill", consumes = ["application/json"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun prefillDocument(  @RequestBody request: ApiRequest): String {
        auditlogger.log("previewDocument", request.aktoerId ?: "", request.toAudit())

        logInfo(request)

        if(request.gjenny && request.avdodfnr == null) {
            return prefillGjennyService.prefillGjennySedtoJson(request)
        }

        return prefillService.prefillSedtoJson(request)
    }

    @PostMapping("sed/prefill/gjenny", consumes = ["application/json"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun prefillGjennyDocument(@RequestBody request: ApiRequest): String {
        auditlogger.log("previewDocument", request.aktoerId ?: "", request.toAudit())
        logInfo(request)
        return prefillGjennyService.prefillGjennySedtoJson(request)

    }

    private fun logInfo(request: ApiRequest) {
        logger.info("Prefiller : ${request.sed}, gjenny: ${request.gjenny}, bucType: ${request.buc}")
        logger.debug("Prefiller : $request")
    }
}
