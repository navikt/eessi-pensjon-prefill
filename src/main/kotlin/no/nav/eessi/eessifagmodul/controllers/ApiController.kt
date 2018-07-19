package no.nav.eessi.eessifagmodul.controllers

import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.models.FrontendRequest
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.createSED
import no.nav.eessi.eessifagmodul.preutfyll.Preutfylling
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api")
class ApiController(private val euxService: EuxService, private val preutfylling: Preutfylling) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(ApiController::class.java) }


    @ApiOperation("viser en oppsumering av SED preutfylling. Før innsending til EUX Basis")
    @PostMapping("/confirm")
    fun confirmDocument(@RequestBody request: FrontendRequest): SED {

        val sed = createPreutfyltSED(request)

        val sedjson = mapAnyToJson(sed, true)
        logger.debug("SED : $sedjson")

        return sed
    }

    @ApiOperation("Kjører prosess OpprettBuCogSED på EUX for å få opprette dokument")
    @PostMapping("/create")
    fun createDocument(@RequestBody request: FrontendRequest): String {

        val fagSaknr = request.caseId!! // = "EESSI-PEN-123"
        //hack only one is selected and used
        if (request.institutions == null || request.institutions.isEmpty()  ) {
            throw IllegalArgumentException("Mangler Mottaker eller Mottakere")
        }
        val mottaker = request.institutions[0].institution!! // = "DUMMY"
        val bucType = request.buc!! // = "P_BUC_06" //P6000
        val korrid = UUID.randomUUID()

        val sed = createPreutfyltSED(request)
        val sedAsJson = mapAnyToJson(sed, true)

        logger.debug("Følgende jsonSED blir sendt : $sedAsJson")

        val euSaksnr = euxService.createCaseAndDocument(
                jsonPayload = sedAsJson,
                fagSaknr = fagSaknr,
                mottaker = mottaker,
                bucType = bucType,
                korrelasjonID = korrid.toString()
        )!!

        logger.debug("(rina) caseid:  $euSaksnr")
        return "{\"euxcaseid\":\"$euSaksnr\"}"
    }

    private fun createPreutfyltSED(request: FrontendRequest):SED {
        if (request.sed != null) {
//            if (request.pinid == null || request.pinid?.length != 13 || request.caseId == null) {
//                logger.debug("Må vel avlutte preutfylling dersom vi ikke har aktoerid eller caseid (saksnr)? det vil vel aldri skje?")
//                throw IllegalArgumentException("Mangler AktoerID eller Saksnr")
//            }
            if ("P6000" == request.sed) {
                val utfyll = preutfylling.preutfylling(request)
                return utfyll.sed
            } else if ("P2000" == request.sed) {
                return createSED(sedName = request.sed)
            } else {
                val utfyll = preutfylling.preutfylling(request)
                val sed: SED = utfyll.sed
                sed.sed = "P6000"
                return sed
            }
        }
        throw IllegalArgumentException("Mangler SED, eller ugyldig SED")
    }

}