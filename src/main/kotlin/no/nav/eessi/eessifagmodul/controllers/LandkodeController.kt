package no.nav.eessi.eessifagmodul.controllers

import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.services.LandkodeService
import no.nav.security.oidc.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController


@Unprotected
@RestController
class LandkodeController(private val landkodeService: LandkodeService) {

    private val logger = LoggerFactory.getLogger(LandkodeController::class.java)

    @ApiOperation("Henter liste over landkoder av ISO Alpha2 standard")
    @GetMapping("/landkoder")
    fun getLandKoder(): String {
        logger.info("Henter landkoder")
        return landkodeService.hentAlleLandkoder()
    }


    @ApiOperation("Henter ut land ISO Alpha3 standard hvor nøkkel er ISO Alpha2")
    @GetMapping("landkoder/{land2}/land3")
    fun getLandKoderAlpha3(@PathVariable("land2", required = true) land2: String): String? {
        logger.info("Henter landkoder")
        return landkodeService.finnLandkode3(land2)
    }

    @ApiOperation("Henter ut land ISO Alpha2 standard hvor nøkkel er ISO Alpha3")
    @GetMapping("landkoder/{land3}/land2")
    fun getLandKoderAlpha2(@PathVariable("land3", required = true) land3: String): String? {
        logger.info("Henter landkoder")
        return landkodeService.finnLandkode2(land3)
    }


}

