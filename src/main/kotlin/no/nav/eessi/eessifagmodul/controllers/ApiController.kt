package no.nav.eessi.eessifagmodul.controllers

import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.preutfyll.Preutfylling
import no.nav.eessi.eessifagmodul.preutfyll.PreutfyllingPersonFraTPS
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.eessi.eessifagmodul.utils.logger
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api")
class ApiController(private val euxService: EuxService, private val preutfylling: Preutfylling) {

    @ApiOperation("bekrefter SED preutfylling før innending til eux-bais(rina)")
    @PostMapping("/confirm")
    fun confirmDocument(@RequestBody request: FrontendRequest): SED {

        return createPreutfyltSED(request)

     }

    @ApiOperation("kjører prosess OpprettBuCogSED på EUX for å få dokuemt opprett i Rina")
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

    //1000042667232
    private fun createPreutfyltSED(request: FrontendRequest):SED {
        if (request.sed != null) {
            if (request.pinid == null || request.pinid?.length != 13) {
                request.pinid = ""
                logger.debug("Satt request pinid til \"\" da den var null")
                //throw IllegalArgumentException("Mangler AktoerID for person")
            }
            if ("P6000" == request.sed) {
                val utfyll = preutfylling.preutfylling(request)
                return utfyll.sed
            } else if ("P2000" == request.sed) {
                return createSED(sedName = request.sed)
            }
        }
        throw IllegalArgumentException("Mangler SED, eller ugyldig SED")
    }

}