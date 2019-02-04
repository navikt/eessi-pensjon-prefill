package no.nav.eessi.eessifagmodul.controllers

import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.services.PostnummerService
import no.nav.security.oidc.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@Unprotected
@RestController
class PostkodeController(private val postnummerService: PostnummerService) {

    private val logger = LoggerFactory.getLogger(LandkodeController::class.java)

    @ApiOperation("Henter ut Poststed med postnr som nøkkel")
    @GetMapping("postnummer/{postnr}/sted")
    fun getLandKoderAlpha3(@PathVariable("postnr", required = true) postnr: String): String? {
        logger.info("Henter poststed fra postnr: $postnr")
        return postnummerService.finnPoststed(postnr)
    }


}