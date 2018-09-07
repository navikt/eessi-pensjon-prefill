package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.models.IkkeGyldigKallException
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.services.LandkodeService
import no.nav.eessi.eessifagmodul.services.PrefillService
import no.nav.eessi.eessifagmodul.services.aktoerregister.AktoerregisterException
import no.nav.eessi.eessifagmodul.services.aktoerregister.AktoerregisterService
import no.nav.eessi.eessifagmodul.services.eux.EuxService
import no.nav.security.oidc.api.Protected
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*


@Protected
@RestController
@RequestMapping("/api")
class ApiController(private val euxService: EuxService, private val prefillService: PrefillService,  private val aktoerregisterService: AktoerregisterService) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(ApiController::class.java) }

    @Autowired
    //TODO hører denne til her eller egen controller?
    lateinit var landkodeService: LandkodeService

    @ApiOperation("Henter liste over landkoder av ISO Alpha2 standard")
    @PostMapping("/landkoder")
    //TODO hører denne til her eller egen controller?
    fun getLandKoder(): List<String> {
        return landkodeService.hentLandkoer2()
    }

    @ApiOperation("viser en oppsumering av SED prefill. Før innsending til EUX Basis")
    @PostMapping("/sed/confirm")
    fun confirmDocument(@RequestBody request: ApiRequest): SED {

        return prefillService.prefillSed(buildPrefillDataModel(request)).sed
    }

    @ApiOperation("sendSed send current sed")
    @PostMapping("/sed/send")
    fun sendSed(@RequestBody request: ApiRequest): Boolean {

        val euxCaseId = request.euxCaseId ?: throw IkkeGyldigKallException("Mangler euxCaseID (RINANR)")
        val sed =  request.sed ?: throw IkkeGyldigKallException("Mangler SED")
        val korrid = UUID.randomUUID().toString()
        return euxService.sendSED(euxCaseId, sed, korrid)
    }

    @ApiOperation("henter ut en SED fra et eksisterende Rina document. krever unik dokumentid fra valgt SED")
    @GetMapping("/sed/get/{rinanr}/{documentid}")
    fun getDocument(@PathVariable("rinanr", required = true) rinanr: String, @PathVariable("documentid", required = true) documentid: String): SED {

        return euxService.fetchSEDfromExistingRinaCase(rinanr, documentid)

    }

    @ApiOperation("sletter SED fra et eksisterende Rina document. krever unik dokumentid fra valgt SED")
    @GetMapping("/sed/delete/{rinanr}/{sed}/{documentid}")
    fun deleteDocument(@PathVariable("rinanr", required = true) rinanr: String, @PathVariable("sed", required = true) sed: String, @PathVariable("documentid", required = true) documentid: String): HttpStatus {

        return euxService.deleteSEDfromExistingRinaCase(rinanr, documentid)
    }

    @ApiOperation("legge til SED på et eksisterende Rina document. kjører preutfylling")
    @PostMapping("/sed/add")
    fun addDocument(@RequestBody request: ApiRequest): String {

        return prefillService.prefillAndAddSedOnExistingCase(buildPrefillDataModel(request)).euxCaseID

    }

    @ApiOperation("Kjører prosess OpprettBuCogSED på EUX for å få opprette et RINA dokument med en SED")
    @PostMapping("/buc/create")
    fun createDocument(@RequestBody request: ApiRequest): String {

        return prefillService.prefillAndCreateSedOnNewCase(buildPrefillDataModel(request)).euxCaseID

    }

    //validatate request and convert to PrefillDataModel
    fun buildPrefillDataModel(request: ApiRequest): PrefillDataModel {
        return when  {
            request.caseId == null -> throw IkkeGyldigKallException("Mangler Saksnummer")
            request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
            request.buc == null -> throw IkkeGyldigKallException("Mangler BUC")
            request.subjectArea == null -> throw IkkeGyldigKallException("Mangler Subjekt/Sektor")
            request.pinid == null -> throw IkkeGyldigKallException("Mangler AktoerID")
            request.institutions == null -> throw IkkeGyldigKallException("Mangler Institusjoner")

        //Denne validering og utfylling kan benyttes på SED P2000 og P6000
            validsed(request.sed , "P2000,P2200,P6000,P5000") -> {
                val pinid = hentAktoerIdPin(request.pinid)

                PrefillDataModel().build(
                        caseId = request.caseId,
                        buc = request.buc,
                        subject = request.subjectArea,
                        sedID = request.sed,
                        aktoerID = request.pinid,
                        pinID = pinid,
                        institutions = request.institutions
                )
            }
        //denne validering og utfylling kan kun benyttes på SED P4000
            validsed(request.sed, "P4000") -> {
                if (request.payload == null) { throw IkkeGyldigKallException("Mangler metadata, payload") }
                if (request.euxCaseId == null) { throw IkkeGyldigKallException("Mangler euxCaseId (RINANR)") }
                val pinid = hentAktoerIdPin(request.pinid)

                PrefillDataModel().build(
                        caseId = request.caseId,
                        buc = request.buc,
                        subject = request.subjectArea,
                        sedID = request.sed,
                        aktoerID = request.pinid,
                        pinID = pinid,
                        institutions = request.institutions,
                        payload = request.payload,
                        euxcaseId = request.euxCaseId
                )
            }
            else -> throw IkkeGyldigKallException("Mangler SED, eller ugyldig type SED")
        }

    }

    private fun validsed(sed: String, validsed: String) : Boolean {
        val result: List<String> = validsed.split(",").map { it.trim() }
        return result.contains(sed)
    }

    @Throws(AktoerregisterException::class)
    fun hentAktoerIdPin(aktorid: String): String {
        if (aktorid.isBlank()) throw IkkeGyldigKallException("Mangler AktorId")
        return aktoerregisterService.hentGjeldendeNorskIdentForAktorId(aktorid)
    }

    //kommer fra frontend
    //{"institutions":[{"NO:"DUMMY"}],"buc":"P_BUC_06","sed":"P6000","caseId":"caseId","subjectArea":"pensjon","actorId":"2323123"}
    data class ApiRequest(
            //sector
            val subjectArea: String? = null,
            //PEN-saksnummer
            val caseId: String? = null,
            val buc: String? = null,
            val sed : String? = null,
            //mottakere
            val institutions: List<InstitusjonItem>? = null,
            @JsonProperty("actorId")
            //aktoerregister
            val pinid: String? = null,
            @JsonProperty("dodactorId")
            val dodpinid: String? = null,
            //mere maa legges til..
            val euxCaseId: String? = null,
            //partpayload json/sed
            val payload: String? = null,
            val sendsed: Boolean? = null,
            val mockSED: Boolean? = null
    )


}