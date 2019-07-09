package no.nav.eessi.pensjon.api.arkiv

import io.swagger.annotations.ApiOperation
import no.nav.eessi.pensjon.services.arkiv.SafException
import no.nav.eessi.pensjon.services.arkiv.SafService
import no.nav.eessi.pensjon.services.arkiv.VariantFormat
import no.nav.eessi.pensjon.utils.errorBody
import no.nav.security.oidc.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@Protected
@RestController
@RequestMapping("/saf")
class SafController(private val safService: SafService) {

    private val logger = LoggerFactory.getLogger(SafController::class.java)

    @ApiOperation("Henter metadata for alle dokumenter i alle journalposter for en gitt aktørid")
    @GetMapping("/metadata/{aktoerId}")
    fun hentDokumentMetadata(@PathVariable("aktoerId", required = true) aktoerId: String): ResponseEntity<String> {
        logger.info("Henter metadata for dokumenter i SAF for aktørid: $aktoerId")
        return try {
            ResponseEntity.ok().body(safService.hentDokumentMetadata(aktoerId).toJson())
        } catch (ex: SafException) {
            ResponseEntity.status(ex.httpStatus).body(errorBody(ex.message!!, UUID.randomUUID().toString()))
        }
    }

    @ApiOperation("Henter dokumentInnhold for et JOARK dokument")
    @GetMapping("/hentdokument/{journalpostId}/{dokumentInfoId}/{variantFormat}")
    fun getDokumentInnhold(@PathVariable("journalpostId", required = true) journalpostId: String,
                           @PathVariable("dokumentInfoId", required = true) dokumentInfoId: String,
                           @PathVariable("variantFormat", required = true) variantFormat: VariantFormat): ResponseEntity<String> {
        logger.info("Henter dokumentinnhold fra SAF for journalpostId: $journalpostId, dokumentInfoId: $dokumentInfoId")
        return try {
            ResponseEntity.ok().body(safService.hentDokumentInnhold(journalpostId, dokumentInfoId, variantFormat).toJson())
        } catch (ex: SafException) {
            ResponseEntity.status(ex.httpStatus).body(errorBody(ex.message!!, UUID.randomUUID().toString()))
        }
    }
}