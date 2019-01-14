package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.services.LandkodeService
import no.nav.eessi.eessifagmodul.services.PrefillService
import no.nav.eessi.eessifagmodul.services.aktoerregister.AktoerregisterService
import no.nav.eessi.eessifagmodul.services.eux.EuxService
import no.nav.eessi.eessifagmodul.services.personv3.PersonV3Service
import no.nav.security.oidc.api.Protected
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

private val logger = LoggerFactory.getLogger(ApiController::class.java)

@Protected
@RestController
@RequestMapping("/api")
class ApiController(private val euxService: EuxService,
                    private val prefillService: PrefillService,
                    private val aktoerregisterService: AktoerregisterService,
                    private val personService: PersonV3Service) {

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
    @PostMapping("/data/personinfo")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    fun hentPersonInformasjon(@RequestBody request: ApiRequest): Nav? {
        val aktorid = request.aktoerId ?: throw IkkeGyldigKallException("Ingen gyldig aktoerId")

        val dataModel = PrefillDataModel().apply {
            sed = SED.create("P2000")
            penSaksnummer = ""
            personNr = hentAktoerIdPin(aktorid)
        }

        val sed = prefillService.prefillSed(dataModel).sed
        return sed.nav
    }


    @ApiOperation("viser en oppsumering av SED prefill. Før innsending til EUX Basis")
    @PostMapping("/sed/confirm", produces = [MediaType.APPLICATION_JSON_VALUE])
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    fun confirmDocument(@RequestBody request: ApiRequest): SED {
        val confirmsed = prefillService.prefillSed(buildPrefillDataModelConfirm(request)).sed
        //preutfylling av P2000 testing
        //TODO fjernes etter endt testing
        if (confirmsed.sed == "P2000") {
            val p2000 = SED.create("P2000")
            p2000.pensjon = confirmsed.pensjon
            p2000.nav = Nav(krav = confirmsed.nav?.krav)
            p2000.print()
            return p2000
        }
        return confirmsed
    }

    @ApiOperation("sendSed send current sed")
    @PostMapping("/sed/send")
    fun sendSed(@RequestBody request: ApiRequest): Boolean {
        val euxCaseId = request.euxCaseId ?: throw IkkeGyldigKallException("Mangler euxCaseID (RINANR)")
        val sed = request.sed ?: throw IkkeGyldigKallException("Mangler SED")
        val korrid = UUID.randomUUID().toString()
        return euxService.sendSED(euxCaseId, sed, korrid)

    }

    @ApiOperation("henter ut en SED fra et eksisterende Rina document. krever unik dokumentid fra valgt SED")
    @GetMapping("/sed/{rinanr}/{documentid}")
    fun getDocument(@PathVariable("rinanr", required = true) rinanr: String,
                    @PathVariable("documentid", required = true) documentid: String): SED {
        return euxService.fetchSEDfromExistingRinaCase(rinanr, documentid)

    }

    @ApiOperation("sletter SED fra et eksisterende Rina document. krever unik dokumentid fra valgt SED")
    @DeleteMapping("/sed/{rinanr}/{documentid}")
    fun deleteDocument(@PathVariable("rinanr", required = true) rinanr: String,
                       @PathVariable("documentid", required = true) sed: String,
                       @PathVariable("documentid", required = true) documentid: String) {

        return euxService.deleteSEDfromExistingRinaCase(rinanr, documentid)
    }

    @ApiOperation("legge til SED på et eksisterende Rina document. kjører preutfylling")
    @PostMapping("/sed/add")
    fun addDocument(@RequestBody request: ApiRequest): String {
        return prefillService.prefillAndAddSedOnExistingCase(buildPrefillDataModelOnExisting(request)).euxCaseID

    }

    @ApiOperation("Kjører prosess OpprettBuCogSED på EUX for å få opprette et RINA dokument med en SED")
    @PostMapping("/buc/create")
    fun createDocument(@RequestBody request: ApiRequest): String {

        return prefillService.prefillAndCreateSedOnNewCase(buildPrefillDataModelOnNew(request)).euxCaseID

    }

    /**
     * Kaller AktørRegisteret , bytter aktørId mot Fnr/Dnr ,
     * deretter kalles PersonV3 hvor personinformasjon hentes
     *
     * @param aktoerid
     */
    @ApiOperation("henter ut personinformasjon for en aktørId")
    @GetMapping("/{aktoerid}")
    fun getDocument(@PathVariable("aktoerid", required = true) aktoerid: String): ResponseEntity<Personinformasjon> {
        logger.info("Henter personinformasjon for aktørId: $aktoerid")

        val norskIdent: String
        var personresp = HentPersonResponse()

        try {
            norskIdent = aktoerregisterService.hentGjeldendeNorskIdentForAktorId(aktoerid)
            personresp = personService.hentPerson(norskIdent)

        } catch (are: AktoerregisterException) {
            logger.error("Kall til Akørregisteret med aktørId: $aktoerid feilet på grunn av: " + are.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AktoerregisterException::class.simpleName)
        } catch (arife: AktoerregisterIkkeFunnetException) {
            logger.error("Kall til Akørregisteret med aktørId: $aktoerid feilet på grunn av: " + arife.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AktoerregisterException::class.simpleName)
        } catch (sbe: PersonV3SikkerhetsbegrensningException) {
            logger.error("Kall til PersonV3 med aktørId: $aktoerid feilet på grunn av sikkerhetsbegrensning")
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(PersonV3SikkerhetsbegrensningException::class.simpleName)
        } catch (ife: PersonV3IkkeFunnetException) {
            logger.error("Kall til PersonV3 med aktørId: $aktoerid feilet på grunn av person ikke funnet")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(PersonV3IkkeFunnetException::class.simpleName)
        }
        return ResponseEntity.ok(Personinformasjon(personresp.person.personnavn.sammensattNavn))
    }
    //validatate request and convert to PrefillDataModel
    fun buildPrefillDataModelOnExisting(request: ApiRequest): PrefillDataModel {
        return when {
            //request.sakId == null -> throw IkkeGyldigKallException("Mangler Saksnummer")
            request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
            request.aktoerId == null -> throw IkkeGyldigKallException("Mangler AktoerID")
            request.euxCaseId == null -> throw IkkeGyldigKallException("Mangler euxCaseId (RINANR)")

            SEDType.isValidSEDType(request.sed) -> {
                println("ALL SED on existin Rina -> SED: ${request.sed} -> euxCaseId: ${request.sakId}")
                val pinid = hentAktoerIdPin(request.aktoerId)
                PrefillDataModel().apply {
                    penSaksnummer = request.sakId
                    sed = SED.create(request.sed)
                    aktoerID = request.aktoerId
                    personNr = pinid
                    euxCaseID = request.euxCaseId

                    vedtakId = request.vedtakId ?: ""
                    partSedAsJson[request.sed] = request.payload ?: ""
                }
            }
            else -> throw IkkeGyldigKallException("Mangler SED, eller ugyldig type SED")
        }
    }

    //validatate request and convert to PrefillDataModel
    fun buildPrefillDataModelOnNew(request: ApiRequest): PrefillDataModel {
        return when {
            //request.sakId == null -> throw IkkeGyldigKallException("Mangler Saksnummer")
            request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
            request.aktoerId == null -> throw IkkeGyldigKallException("Mangler AktoerID")
            request.buc == null -> throw IkkeGyldigKallException("Mangler BUC")
            request.subjectArea == null -> throw IkkeGyldigKallException("Mangler Subjekt/Sektor")
            request.institutions == null -> throw IkkeGyldigKallException("Mangler Institusjoner")

            //Denne validering og utfylling kan benyttes på SED P2000,P2100,P2200
            SEDType.isValidSEDType(request.sed) -> {
                println("ALL SED on new RinaCase -> SED: ${request.sed}")
                val pinid = hentAktoerIdPin(request.aktoerId)
                PrefillDataModel().apply {
                    penSaksnummer = request.sakId
                    buc = request.buc
                    rinaSubject = request.subjectArea
                    sed = SED.create(request.sed)
                    aktoerID = request.aktoerId
                    personNr = pinid
                    institution = request.institutions
                    vedtakId = request.vedtakId ?: ""
                }
            }
            else -> throw IkkeGyldigKallException("Mangler SED, eller ugyldig type SED")
        }
    }

    //validatate request and convert to PrefillDataModel
    fun buildPrefillDataModelConfirm(request: ApiRequest): PrefillDataModel {
        return when {
            //request.sakId == null -> throw IkkeGyldigKallException("Mangler Saksnummer")
            request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
            request.aktoerId == null -> throw IkkeGyldigKallException("Mangler AktoerID")

            SEDType.isValidSEDType(request.sed) -> {
                PrefillDataModel().apply {
                    penSaksnummer = request.sakId
                    sed = SED.create(request.sed)
                    aktoerID = request.aktoerId
                    personNr = hentAktoerIdPin(request.aktoerId)
                    vedtakId = request.vedtakId ?: ""
                    if (request.payload != null) {
                        partSedAsJson[request.sed] = request.payload
                    }
                }
            }
            else -> throw IkkeGyldigKallException("Mangler SED, eller ugyldig type SED")
        }
    }

    @Throws(AktoerregisterException::class)
    fun hentAktoerIdPin(aktorid: String): String {
        if (aktorid.isBlank()) throw IkkeGyldigKallException("Mangler AktorId")
        return aktoerregisterService.hentGjeldendeNorskIdentForAktorId(aktorid)
    }

    data class ApiRequest(
            val sakId: String,
            val vedtakId: String? = null,
            val kravId: String? = null,
            val aktoerId: String? = null,
            val fnr: String? = null,
            val payload: String? = null,
            val buc: String? = null,
            val sed: String? = null,
            val euxCaseId: String? = null,
            val institutions: List<InstitusjonItem>? = null,
            val subjectArea: String? = null,
            val mockSED: Boolean? = null
    )
}