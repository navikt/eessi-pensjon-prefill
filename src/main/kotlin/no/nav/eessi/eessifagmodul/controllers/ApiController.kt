package no.nav.eessi.eessifagmodul.controllers

import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.eessi.eessifagmodul.utils.logger
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import org.springframework.core.annotation.AliasFor
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.*
import org.springframework.web.bind.annotation.RequestMapping


@RestController
@RequestMapping("/api")
class ApiController(private val euxService: EuxService) {

    @GetMapping("/refreshAll")
    fun refreshAll() {
        euxService.refreshAll()
    }

    //saksid
    @GetMapping("/case/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getCase(@PathVariable(value = "id", required = true) caseid: String): String {
        return "{\"caseId\":\"$caseid\"}"
    }

    @ApiOperation("henter liste av alle tilgjengelige BuC fra EUX")
    @GetMapping("/bucs", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getBucs(): List<String> {
        return euxService.getCachedBuCTypePerSekor()
    }

    @GetMapping("/seds", "/seds/{buc}")
    fun getSeds(@PathVariable(value = "buc", required = false) buc: String?): List<String> {
        return euxService.getAvailableSEDonBuc(buc)
    }

    @ApiOperation("henter liste av alle tilgjengelige instusjoner fra EUX")
    @GetMapping("/institutions")
    fun getInstitutions(@RequestParam(value = "buc", required = false, defaultValue = "") buc: String,
                        @RequestParam(value = "landkode", required = false, defaultValue  = "") landkode: String): List<String> {
        return euxService.getCachedInstitusjoner()
        //return euxService.getInstitusjoner(buc, landkode)
    }

    @GetMapping("/institutions/{countrycode}")
    fun getInstitutionsWithCountry(@PathVariable(value = "countrycode", required = false) landkode: String = ""): List<String> {
        return euxService.getInstitusjoner("",landkode)
    }

    @AliasFor("/create")
    @PostMapping("/casesubmit")
    fun createDocumentSubmit(@RequestBody request: FrontendRequest): String {
        return createDocument(request)
    }

    @ApiOperation("kjører prosess OpprettBuCogSED på EUX for å få dokuemt opprett i Rina")
    @PostMapping("/create")
    fun createDocument(@RequestBody request: FrontendRequest): String {

        val fagSaknr = request.caseId!! // = "EESSI-PEN-123"
        val mottaker = request.institution!! // = "DUMMY"
        val bucType = request.buc!! // = "P_BUC_06" //P6000
        val korrid = UUID.randomUUID()

        //still mock P6000?
        //val sed : SED?
        val sed: SED
        if (request.sed != null) {
            sed = createSED(request.sed)
            if (sed.sed == "P6000") {
                sed.nav = Nav(
                        bruker = Bruker(
                                person = Person(
                                fornavn = "Fornavn",
                                kjoenn = "f",
                                foedselsdato = "1967-12-01",
                                etternavn = "Etternavn"
                                )
                        )
                )
                sed.pensjon = Pensjon(
                    gjenlevende = Gjenlevende(
                        person = Person(
                            fornavn = "Fornavn",
                            kjoenn = "f",
                            foedselsdato = "1967-12-01",
                            etternavn = "Etternavn"
                        )
                    )
                )
                //sed.pensjon = PensjonMock().genererMockData()
                //sed.nav = NavMock().genererNavMock()
           } else if (sed.sed == "P2000") {
                sed.nav = Nav()
                sed.pensjon = Pensjon()
            }
//                sed.nav = Nav(
//                        bruker = Bruker(
//                                person = Person(
//                                        fornavn = "Gul",
//                                        kjoenn = "f",
//                                        foedselsdato = "1967-12-01",
//                                        etternavn = "Konsoll"
//                                )
//                        )
//                )
//                sed.pensjon = Pensjon(
//                    gjenlevende = Gjenlevende(
//                            person = Person(
//                                    fornavn = "Gul",
//                                    kjoenn = "f",
//                                    foedselsdato = "1967-12-01",
//                                    etternavn = "Konsoll"
//                            )
//                    )
//                )
//            }
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