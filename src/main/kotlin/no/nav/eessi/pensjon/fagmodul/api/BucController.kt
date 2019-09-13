package no.nav.eessi.pensjon.fagmodul.api

import io.swagger.annotations.ApiOperation
import no.nav.eessi.pensjon.fagmodul.eux.BucAndSedView
import no.nav.eessi.pensjon.fagmodul.eux.BucUtils
import no.nav.eessi.pensjon.fagmodul.eux.EuxService
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.Rinasak
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.Vedlegg
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.Buc
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.Creator
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.ShortDocumentItem
import no.nav.eessi.pensjon.helper.AktoerIdHelper
import no.nav.eessi.pensjon.services.arkiv.SafService
import no.nav.eessi.pensjon.services.arkiv.VariantFormat
import no.nav.eessi.pensjon.utils.errorBody
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.successBody
import no.nav.security.oidc.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*


@Protected
@RestController
@RequestMapping("/buc")
class BucController(private val euxService: EuxService,
                    private val safService: SafService,
                    private val aktoerIdHelper: AktoerIdHelper,
                    private val fasitEnvName: String) {

    private val logger = LoggerFactory.getLogger(BucController::class.java)

    @ApiOperation("henter liste av alle tilgjengelige BuC-typer")
    @GetMapping("/bucs", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getBucs() = EuxService.initSedOnBuc().keys.map { it }.toList()

    @ApiOperation("Henter opp hele BUC på valgt caseid")
    @GetMapping("/{rinanr}")
    fun getBuc(@PathVariable(value = "rinanr", required = true) rinanr: String): Buc {
        logger.debug("Henter ut hele Buc data fra rina via eux-rina-api")
        return euxService.getBuc(rinanr)
    }

    @ApiOperation("Viser prosessnavnet (f.eks P_BUC_01) på den valgte BUCen")
    @GetMapping("/{rinanr}/name")
    fun getProcessDefinitionName(@PathVariable(value = "rinanr", required = true) rinanr: String): String? {

        logger.debug("Henter ut definisjonsnavn (type type) på valgt Buc")
        return BucUtils(euxService.getBuc(rinanr)).getProcessDefinitionName()
    }

    @ApiOperation("Henter opp den opprinelige inststusjon på valgt caseid (type)")
    @GetMapping("/{rinanr}/creator")
    fun getCreator(@PathVariable(value = "rinanr", required = true) rinanr: String): Creator? {

        logger.debug("Henter ut Creator på valgt Buc")
        return BucUtils(euxService.getBuc(rinanr)).getCreator()
    }

    @ApiOperation("Henter opp creator countrycode (type)")
    @GetMapping("/{rinanr}/creator/countryCode")
    fun getCreatorCountryCode(@PathVariable(value = "rinanr", required = true) rinanr: String): String? {

        logger.debug("Henter ut CountryCode på Creator på valgt Buc")
        return mapAnyToJson(BucUtils(euxService.getBuc(rinanr)).getCreatorContryCode())
    }

    @ApiOperation("Henter opp internationalid på caseid (type)")
    @GetMapping("/{rinanr}/internationalId")
    fun getInternationalId(@PathVariable(value = "rinanr", required = true) rinanr: String): String? {

        logger.debug("Henter ut InternationalId på valgt Buc")
        return BucUtils(euxService.getBuc(rinanr)).getInternatinalId()
    }

    @ApiOperation("Henter opp den opprinelige inststusjon på valgt caseid (type)")
    @GetMapping("/{rinanr}/allDocuments")
    fun getAllDocuments(@PathVariable(value = "rinanr", required = true) rinanr: String): List<ShortDocumentItem> {

        logger.debug("Henter ut documentId på alle dokumenter som finnes på valgt type")
        return BucUtils(euxService.getBuc(rinanr)).getAllDocuments()
    }

    @ApiOperation("Henter opp mulige aksjon(er) som kan utføres på valgt buc")
    @GetMapping("/{rinanr}/aksjoner")
    fun getMuligeAksjoner(@PathVariable(value = "rinanr", required = true) rinanr: String): List<String> {
        logger.debug("Henter ut muligeaksjoner på valgt buc ${rinanr}")
        return BucUtils(euxService.getBuc(rinanr)).getAksjonListAsString()
    }

    @ApiOperation("Henter ut en liste over saker på valgt aktoerid. ny api kall til eux")
    @GetMapping("/rinasaker/{aktoerId}")
    fun getRinasaker(@PathVariable("aktoerId", required = true) aktoerId: String): List<Rinasak> {

        logger.debug("henter rinasaker på valgt aktoerid: $aktoerId")
        val fnr = aktoerIdHelper.hentPinForAktoer(aktoerId)
        val rinaSakIderFraDokumentMetadata = safService.hentRinaSakIderFraDokumentMetadata(aktoerId)
        return euxService.getRinasaker(fnr, rinaSakIderFraDokumentMetadata, fasitEnvName)
    }

    @ApiOperation("Henter ut en json struktur for type og sed menyliste for ui. ny api kall til eux")
    @GetMapping("/detaljer/{aktoerid}", "/detaljer/{aktoerid}/{sakid}", "/detaljer/{aktoerId}/{sakId}/{euxcaseid}")
    fun getBucogSedView(@PathVariable("aktoerid", required = true) aktoerid: String,
                        @PathVariable("sakid", required = false) sakid: String? = "",
                        @PathVariable("euxcaseid", required = false) euxcaseid: String? = ""): List<BucAndSedView> {
        logger.debug("1 prøver å dekode til fnr fra aktoerid: $aktoerid")
        val fnr = aktoerIdHelper.hentPinForAktoer(aktoerid)
        val rinaSakIderFraDokumentMetadata = safService.hentRinaSakIderFraDokumentMetadata(aktoerid)
        return euxService.getBucAndSedView(euxService.getRinasaker(fnr, rinaSakIderFraDokumentMetadata, fasitEnvName), aktoerid)
    }

    @ApiOperation("Henter ut en json struktur for type og sed menyliste for ui. ny api kall til eux")
    @GetMapping("/enkeldetalj/{euxcaseid}")
    fun getSingleBucogSedView(@PathVariable("euxcaseid", required = true) euxcaseid: String): BucAndSedView {
        logger.debug("1 prøver å hente en enkel buc på : $euxcaseid")
        return euxService.getSingleBucAndSedView(euxcaseid)
    }

    @ApiOperation("Oppretter ny tom BUC i RINA via eux-api. ny api kall til eux")
    @PostMapping("/{buctype}")
    fun createBuc(@PathVariable("buctype", required = true) buctype: String): BucAndSedView {
        logger.debug("Prøver å opprette en ny BUC i RINA av type: $buctype")

        //rinaid
        val euxCaseId = euxService.createBuc(buctype)
        logger.info("Mottatt følgende euxCaseId(RinaID): $euxCaseId")

        //create bucDetail back from newly created buc call eux-rina-api to get data.
        val buc = euxService.getBuc(euxCaseId)
        return BucAndSedView.from(buc, "")
    }

    @ApiOperation("Legger til et vedlegg for det gitte dokumentet")
    @PutMapping("/vedlegg/{aktoerId}/{rinaSakId}/{rinaDokumentId}/{joarkJournalpostId}/{joarkDokumentInfoId}/{variantFormat}")
    fun putVedleggTilDokument(@PathVariable("aktoerId", required = true) aktoerId: String,
                              @PathVariable("rinaSakId", required = true) rinaSakId: String,
                              @PathVariable("rinaDokumentId", required = true) rinaDokumentId: String,
                              @PathVariable("joarkJournalpostId", required = true) joarkJournalpostId: String,
                              @PathVariable("joarkDokumentInfoId", required = true) joarkDokumentInfoId : String,
                              @PathVariable("variantFormat", required = true) variantFormat : VariantFormat) : ResponseEntity<String> {
        logger.debug("Legger til vedlegg: joarkJournalpostId: $joarkJournalpostId, joarkDokumentInfoId $joarkDokumentInfoId, variantFormat: $variantFormat til " +
                "rinaSakId: $rinaSakId, rinaDokumentId: $rinaDokumentId")

        return try {
            val dokument = safService.hentDokumentInnhold(joarkJournalpostId, joarkDokumentInfoId, variantFormat)
            euxService.leggTilVedleggPaaDokument(aktoerId,
                    rinaSakId,
                    rinaDokumentId,
                    Vedlegg(filInnhold = dokument.filInnhold, filnavn = dokument.fileName),
                    dokument.contentType.split("/")[1])
            return ResponseEntity.ok().body(successBody())
        } catch(ex: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody(ex.message!!, UUID.randomUUID().toString()))
        }
    }
}
