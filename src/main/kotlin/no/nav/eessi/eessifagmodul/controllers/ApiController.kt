package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.createSED
import no.nav.eessi.eessifagmodul.preutfyll.PreutfyllingPerson
import no.nav.eessi.eessifagmodul.preutfyll.UtfyllingData
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
class ApiController(private val euxService: EuxService, private val preutfyllingPerson: PreutfyllingPerson) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(ApiController::class.java) }


    @ApiOperation("viser en oppsumering av SED preutfyll. Før innsending til EUX Basis")
    @PostMapping("/confirm")
    fun confirmDocument(@RequestBody request: RequestApi): SED {

        val sed = createPreutfyltSED(request)

        val sedjson = mapAnyToJson(sed, true)
        logger.debug("SED : $sedjson")

        return sed
    }

    @ApiOperation("Kjører prosess OpprettBuCogSED på EUX for å få opprette dokument")
    @PostMapping("/create")
    fun createDocument(@RequestBody request: RequestApi): String {

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

    //validaring og preutfylling
    private fun createPreutfyltSED(request: RequestApi):SED {
        return when  {
            request.caseId == null -> throw IllegalArgumentException("Mangler Saksnummer")
            request.sed == null -> throw IllegalArgumentException("Mangler SED")
            request.buc == null -> throw IllegalArgumentException("Mangler BUC")
            request.subjectArea == null -> throw IllegalArgumentException("Mangler Subjekt/Sektor")
            request.pinid == null -> throw IllegalArgumentException("Mangler AktoerID")
            request.institutions == null -> throw IllegalArgumentException("Mangler Institusjoner")
            request.sed == "P2000" -> createSED(sedName = request.sed)
            request.sed == "P6000" -> preutfyllingPerson.preutfyll(
                    utfyllingData = UtfyllingData()
                            .build(
                                    caseId = request.caseId,
                                    buc = request.buc,
                                    subject = request.subjectArea,
                                    sedID = request.sed,
                                    aktoerID = request.pinid,
                                    data = request.institutions
                            )
            )
            else -> throw IllegalArgumentException("Mangler SED, eller ugyldig type SED")
        }
    }

    //kommer fra frontend
    //{"institutions":[{"NO:"DUMMY"}],"buc":"P_BUC_06","sed":"P6000","caseId":"caseId","subjectArea":"pensjon","actorId":"2323123"}
    data class RequestApi(
            //sector
            val subjectArea: String? = null,
            //PEN-saksnummer
            val caseId: String? = null,
            val buc: String? = null,
            val sed : String? = null,
            //mottakere
            val institutions: List<no.nav.eessi.eessifagmodul.models.InstitusjonItem>? = null,
            @JsonProperty("actorId")
            //aktoerid
            val pinid: String? = null
    )


}