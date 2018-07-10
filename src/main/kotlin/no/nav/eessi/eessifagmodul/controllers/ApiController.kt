package no.nav.eessi.eessifagmodul.controllers

import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.preutfyll.Preutfylling
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.eessi.eessifagmodul.utils.logger
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*
import org.springframework.web.bind.annotation.RequestMapping


@RestController
@RequestMapping("/api")
class ApiController(private val euxService: EuxService) {


    @ApiOperation("confirm SED-prutfylling før innending til eux-bais(rina)")
    @PostMapping("/confirm")
    fun confirmDocument(@RequestBody request: FrontendRequest): SED {
        val sed: SED
        if (request.sed != null) {
            sed = createSED(request.sed)
            if (sed.sed == "P6000") {
                val preutfyll = Preutfylling()
                val utfyll = preutfyll.preutfylling(sed)
                sed.nav = utfyll.sed.nav
                sed.pensjon = utfyll.sed.pensjon
            } else if (sed.sed == "P2000") {
                sed.nav = Nav()
                sed.pensjon = Pensjon()
            }
        } else {
            throw IllegalArgumentException("Mangler SED, eller ugyldig SED")
        }
        return sed
    }

    @ApiOperation("kjører prosess OpprettBuCogSED på EUX for å få dokuemt opprett i Rina")
    @PostMapping("/create")
    fun createDocument(@RequestBody request: FrontendRequest): String {
        val fagSaknr = request.caseId!! // = "EESSI-PEN-123"
        //hack only one is selected and used
        val mottaker = request.institutions!![0].institution!! // = "DUMMY"
        val bucType = request.buc!! // = "P_BUC_06" //P6000
        val korrid = UUID.randomUUID()

        //still mock P6000?
        //val sed : SED?
        val sed: SED
        if (request.sed != null) {
            sed = createSED(request.sed)
            if (sed.sed == "P6000") {
                val preutfyll = Preutfylling()
                val utfyll = preutfyll.preutfylling(sed)
                sed.nav = utfyll.sed.nav
                sed.pensjon = utfyll.sed.pensjon
           } else if (sed.sed == "P2000") {
                sed.nav = Nav()
                sed.pensjon = Pensjon()
            }
        } else {
            throw IllegalArgumentException("Mangler SED, eller ugyldig SED")
        }
        val sedAsJson = mapAnyToJson(sed, true)
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


}