package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.clients.aktoerid.AktoerIdClient
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.PrefillSED
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.services.RinaActions
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.eessi.eessifagmodul.services.LandkodeService
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping("/api")
class ApiController(private val euxService: EuxService, private val prefillSED: PrefillSED, private val aktoerIdClient: AktoerIdClient) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(ApiController::class.java) }

    @Autowired
    lateinit var landkodeService: LandkodeService

    @Autowired
    lateinit var rinaActions: RinaActions

    @ApiOperation("Henter liste over landkoder av ISO Alpha2 standard")
    @PostMapping("/landkoder")
    fun getLandKoder(): List<String> {
        return landkodeService.hentLandkoer2()
    }

    @ApiOperation("viser en oppsumering av SED prefill. Før innsending til EUX Basis")
    @PostMapping("/confirm")
    fun confirmDocument(@RequestBody request: ApiRequest): SED {

        val data = createPreutfyltSED(request)
        val sed = data.sed

        val sedjson = mapAnyToJson(sed, true)
        logger.debug("SED : $sedjson")

        return sed
    }

    @ApiOperation("sendSed send current sed")
    @PostMapping("/sendsed")
    fun sendSed(@RequestBody request: ApiRequest): Boolean {

        val rinanr = request.euxCaseId ?: throw IkkeGyldigKallException("Mangler euxCaseID (RINANR)")
        val sed =  request.sed ?: throw IkkeGyldigKallException("Mangler SED")
        val korrid = UUID.randomUUID()

        return euxService.sendSED(rinanr, sed, korrid.toString())
    }

    @ApiOperation("legge til SED på et eksisterende Rina document. kjører preutfylling")
    @PostMapping("/addsed")
    fun addDocument(@RequestBody request: ApiRequest): String {
        //vi må ha mer fra frontend // backend..

        // Trenger RinaNr fra tidligere (opprettBucOgSed) gir oss orginale rinanr.
        // payload fra f.eks P4000,P2000,xx dene er vel da bare delevis.
        // dette legges vel til i ApiRequest model som payload.

        val rinanr = request.euxCaseId ?: throw IkkeGyldigKallException("Mangler euxCaseId (RINANR)")
        val korrid = UUID.randomUUID()

        val data = createPreutfyltSED(request)
        val sed = data.sed
        val sedAsJson = mapAnyToJson(sed, true)

        if (rinaActions.canCreate(data.getSEDid(), rinanr)) {
            euxService.createSEDonExistingRinaCase(sedAsJson, rinanr, korrid.toString())
            //ingen ting tilbake.. sjekke om alt er ok?
            //val aksjon = euxService.getPossibleActions(rinanr)
            if (rinaActions.canUpdate(data.getSEDid() , rinanr)) {
                return rinanr
            }
            throw SedDokumentIkkeOpprettetException("SED dokument feilet ved opprettelse ved RINANR: $rinanr")
        }
        throw SedDokumentIkkeGyldigException("Kan ikke opprette følgende  SED: ${sed.sed} på RINANR: $rinanr")

    }

    @ApiOperation("Kjører prosess OpprettBuCogSED på EUX for å få opprette dokument")
    @PostMapping("/create")
    fun createDocument(@RequestBody request: ApiRequest): String {

        val korrid = UUID.randomUUID()
        val data = createPreutfyltSED(request)
        val sed = data.sed

        val fagSaknr = data.penSaksnummer // = "EESSI-PEN-123"
        val bucType = data.buc // = "P_BUC_06" //P6000

        val mottaker = getFirstInstitution(data.getInstitutionsList())
        val sedAsJson = mapAnyToJson(sed, true)

        logger.debug("Følgende jsonSED blir sendt : $sedAsJson")

        val euSaksnr = euxService.createCaseAndDocument(
                jsonPayload = sedAsJson,
                fagSaknr = fagSaknr,
                mottaker = mottaker,
                bucType = bucType,
                korrelasjonID = korrid.toString()
        )
        logger.debug("(rina) caseid:  $euSaksnr")
        if (rinaActions.canUpdate(data.getSEDid(), euSaksnr)) {
            if (request.sendsed != null && request.sendsed == true) {
                val result = euxService.sendSED(euSaksnr, data.getSEDid(), korrid.toString())
                if (result == false) {
                    throw SedDokumentIkkeSendtException("SED ikke sendt. muligens opprettet på RINANR: $euSaksnr")
                }
            }
            return "{\"euxcaseid\":\"$euSaksnr\"}"
        }
        throw SedDokumentIkkeOpprettetException("SED dokument feilet ved opprettelse ved RINANR: $euSaksnr")
    }

    //muligens midlertidig metode for å sende kun en mottaker til EUX.
    private fun getFirstInstitution(institutions: List<InstitusjonItem>): String {
        institutions.forEach {
            return it.institution ?: throw IkkeGyldigKallException("institujson kan ikke være tom")
        }
        throw IkkeGyldigKallException("Mangler mottaker register (InstitusjonItem)")
    }

    //validatate request and convert to PrefillDataModel
    fun createPrefillData(request: ApiRequest): PrefillDataModel {
        return when  {
            request.caseId == null -> throw IkkeGyldigKallException("Mangler Saksnummer")
            request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
            request.buc == null -> throw IkkeGyldigKallException("Mangler BUC")
            request.subjectArea == null -> throw IkkeGyldigKallException("Mangler Subjekt/Sektor")
            request.pinid == null -> throw IkkeGyldigKallException("Mangler AktoerID")
            request.institutions == null -> throw IkkeGyldigKallException("Mangler Institusjoner")

        //Denne validering og utfylling kan benyttes på SED P2000 og P6000
            validsed(request.sed , "P2000,P6000,P5000") -> {
                PrefillDataModel().build(
                        caseId = request.caseId,
                        buc = request.buc,
                        subject = request.subjectArea,
                        sedID = request.sed,
                        aktoerID = request.pinid,
                        pinID = hentAktoerIdPin(request.pinid),
                        institutions = request.institutions
                )
            }
        //denne validering og utfylling kan kun benyttes på SED P4000
            validsed(request.sed, "P4000") -> {
                if (request.payload == null) { throw IkkeGyldigKallException("Mangler metadata, payload") }
                if (request.euxCaseId == null) { throw IkkeGyldigKallException("Mangler euxCaseId (RINANR)") }
                PrefillDataModel().build(
                        caseId = request.caseId,
                        buc = request.buc,
                        subject = request.subjectArea,
                        sedID = request.sed,
                        aktoerID = request.pinid,
                        pinID = hentAktoerIdPin(request.pinid),
                        institutions = request.institutions,
                        payload = request.payload,
                        euxcaseId = request.euxCaseId
                )
            }
            else -> throw IkkeGyldigKallException("Mangler SED, eller ugyldig type SED")
        }

    }

    //Prefill data and create SED (PEN and TPS..)
    fun createPreutfyltSED(data: PrefillDataModel): PrefillDataModel {
        return prefillSED.prefill(data)
    }

    private fun createPreutfyltSED(request: ApiRequest): PrefillDataModel {
        return createPreutfyltSED(createPrefillData(request))
    }

    private fun validsed(sed: String, validsed: String) : Boolean {
        val result: List<String> = validsed.split(",").map { it.trim() }
        return result.contains(sed)
    }

    @Throws(RuntimeException::class)
    fun hentAktoerIdPin(aktorid: String): String {
        if (aktorid.isBlank()) return ""
        return aktoerIdClient.hentPinIdentFraAktorid(aktorid)
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
            //aktoerid
            val pinid: String? = null,
            @JsonProperty("dodactorId")
            val dodpinid: String? = null,
            //mere maa legges til..
            val euxCaseId: String? = null,
            //partpayload json/sed
            val payload: String? = null,
            val sendsed: Boolean? = null
    )


}