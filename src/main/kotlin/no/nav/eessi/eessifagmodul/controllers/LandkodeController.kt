package no.nav.eessi.eessifagmodul.controllers

import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.services.LandkodeService
import no.nav.security.oidc.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

private val logger = LoggerFactory.getLogger(LandkodeController::class.java)

@Protected
@RestController
class LandkodeController(private val landkodeService: LandkodeService) {

    @ApiOperation("Henter liste over landkoder av ISO Alpha2 standard")
    @PostMapping("/landkoder")
    fun getLandKoder(): List<String> {
        logger.info("Henter landkoder")
        return landkodeService.hentLandkode2()
    }
}

