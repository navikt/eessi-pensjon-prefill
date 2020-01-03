package no.nav.eessi.pensjon.fagmodul.eux

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.base.Preconditions
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.BucSedResponse
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.Rinasak
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.Vedlegg
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.Buc
import no.nav.eessi.pensjon.fagmodul.metrics.getCounter
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.sedmodel.Krav
import no.nav.eessi.pensjon.fagmodul.sedmodel.PinItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import no.nav.eessi.pensjon.utils.typeRefs
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Description
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.UnknownHttpStatusCodeException
import org.springframework.web.util.UriComponentsBuilder
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.streams.toList

@Service
@Description("Service class for EuxBasis - eux-cpi-service-controller")
class EuxService(private val euxOidcRestTemplate: RestTemplate,
                 @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())) {

    // Vi trenger denne no arg konstruktøren for å kunne bruke @Spy med mockito
    constructor() : this(RestTemplate(), MetricsHelper(SimpleMeterRegistry()))

    private val logger = LoggerFactory.getLogger(EuxService::class.java)

    private val mapper = jacksonObjectMapper()

    // https://eux-app.nais.preprod.local/swagger-ui.html#/eux-cpi-service-controller/

    @Throws(EuxGenericServerException::class, SedDokumentIkkeOpprettetException::class)
    fun opprettSvarSedOnBuc(navSED: SED, euxCaseId: String, parentDocumentId: String): BucSedResponse {
        val euxUrlpath = "/buc/{RinaSakId}/sed/{DokuemntId}/svar"
        logger.debug("prøver å kontakte eux-rina-api : $euxUrlpath")
        return opprettSed(euxUrlpath, navSED.toJsonSkipEmpty(), euxCaseId, "OpprettSvarSed", "Feil ved opprettSvarSed", parentDocumentId)
    }


    //ny SED på ekisterende type
    @Throws(EuxGenericServerException::class, SedDokumentIkkeOpprettetException::class)
    fun opprettSedOnBuc(navSED: SED, euxCaseId: String): BucSedResponse {
        val euxUrlpath = "/buc/{RinaSakId}/sed"
        return opprettSed(euxUrlpath, navSED.toJsonSkipEmpty(), euxCaseId, "OpprettSed", "Feil ved opprettSed",null)
    }


    //ny SED på ekisterende type eller ny svar SED på ekisternede rina
    @Throws(EuxGenericServerException::class, SedDokumentIkkeOpprettetException::class)
    fun opprettSed(urlPath: String, navSEDjson: String, euxCaseId: String, metricName: String, errorMessage: String, parentDocumentId: String?): BucSedResponse {

        val uriParams = mapOf("RinaSakId" to euxCaseId, "DokuemntId" to parentDocumentId).filter { it.value != null }
        val builder = UriComponentsBuilder.fromUriString(urlPath)
                .queryParam("KorrelasjonsId", UUID.randomUUID().toString())
                .buildAndExpand(uriParams)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        //legger til navsed som json skipper nonemty felter dvs. null
        val httpEntity = HttpEntity(navSEDjson, headers)

        val response = restTemplateErrorhandler(
                {
                    euxOidcRestTemplate.exchange(builder.toUriString(),
                    HttpMethod.POST,
                    httpEntity,
                    String::class.java)
                }
                , euxCaseId
                , metricName
                , errorMessage
        )
        return BucSedResponse(euxCaseId, response.body!!)
    }

    //henter ut sed fra rina med bucid og documentid
    fun getSedOnBuc(euxCaseId: String, sedType: String?): List<SED> {
        logger.info("Prøver å hente ut en BucUtils for type $euxCaseId")
        val docid = getBuc(euxCaseId).documents ?: throw NoSuchFieldException("Fant ikke DocumentsItem")

        val sedlist = mutableListOf<SED>()
        docid.forEach {
            if (sedType != null && sedType == it.type) {
                it.id?.let { id ->
                    sedlist.add(getSedOnBucByDocumentId(euxCaseId, id))
                }
            } else {
                it.id?.let { id ->
                    sedlist.add(getSedOnBucByDocumentId(euxCaseId, id))
                }
            }
        }
        return sedlist
    }

    //henter ut sed fra rina med bucid og documentid
    @Throws(EuxServerException::class, SedDokumentIkkeLestException::class)
    fun getSedOnBucByDocumentIdAsJson(euxCaseId: String, documentId: String): String {
        val path = "/buc/{RinaSakId}/sed/{DokumentId}"
        val uriParams = mapOf("RinaSakId" to euxCaseId, "DokumentId" to documentId)
        val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)

        logger.info("Prøver å kontakte EUX /${builder.toUriString()}")

        val response = restTemplateErrorhandler(
                {
                    euxOidcRestTemplate.exchange(builder.toUriString(),
                            HttpMethod.GET,
                            null,
                            String::class.java)
                }
                , euxCaseId
                , "SedByDocumentId"
                , "Feil ved henting av Sed med DocId: $documentId"
        )
        return  response.body ?: {
            logger.error("Feiler ved lasting av navSed: ${builder.toUriString()}")
            throw SedDokumentIkkeLestException("Feiler ved lesing av navSED, feiler ved uthenting av SED")
        }()
    }

    @Throws(EuxServerException::class, SedDokumentIkkeLestException::class)
    fun getSedOnBucByDocumentId(euxCaseId: String, documentId: String): SED {
        val json = getSedOnBucByDocumentIdAsJson(euxCaseId, documentId)
        return SED.fromJson(json)
    }

    //val benytt denne for å hente ut PESYS sakid (P2000,P2100,P2200,P6000)
    fun hentPESYSsakIdFraRinaSED(euxCaseId: String, documentId: String): String {
        val na = "N/A"

        try {
            val navsed = getSedOnBucByDocumentId(euxCaseId, documentId)

            val eessisak = navsed.nav?.eessisak?.get(0)

            val instnavn = eessisak?.institusjonsnavn ?: na
            if (instnavn.contains("NO")) {
                navsed.nav?.eessisak?.first()?.saksnummer?.let { return it }
            }

        } catch (ex: Exception) {
            logger.warn("Klarte ikke å hente inn SED dokumenter for å lese inn saksnr!")
        }
        return na
    }

    // @ Throws(BucIkkeMottattException::class, EuxServerException::class)
    fun getBuc(euxCaseId: String): Buc {
        logger.info("euxCaseId: $euxCaseId")

        val path = "/buc/{RinaSakId}"
        val uriParams = mapOf("RinaSakId" to euxCaseId)
        val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)

        logger.info("Prøver å kontakte EUX /${builder.toUriString()}")

        val response = restTemplateErrorhandler(
                {
                    euxOidcRestTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    String::class.java)
                }
                ,euxCaseId
                ,"getbuc"
                ,"Feiler ved metode GetBuc. "
        )
        return mapJsonToAny(response.body ?: {
            logger.error("Feil med mapping euxCaseId $euxCaseId")
            throw ServerException("Feil med Buc mapping, euxCaseId $euxCaseId")}(), typeRefs())
    }

    /**
     * sletter et SED doument på RINA.
     * @param euxCaseId  er iden til den aktuelle Buc/Rina sak
     * @param documentId er iden til det unike dokuement/Sed som skal slettes.
     * true hvis alt ok, og sed slettt. Exception error hvis feil.
     */
    @Throws(SedIkkeSlettetException::class, EuxServerException::class)
    fun deleteDocumentById(euxCaseId: String, documentId: String): Boolean {
        val path = "/buc/{RinaSakId}/sed/{DokumentId}"
        val uriParams = mapOf("RinaSakId" to euxCaseId, "DokumentId" to documentId)
        val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)

        try {
            logger.info("Prøver å kontakte EUX /${builder.toUriString()}")

            val response = euxOidcRestTemplate.exchange(builder.toUriString(),
                    HttpMethod.DELETE,
                    null,
                    String::class.java)

            if (response.statusCode.is2xxSuccessful) {
                logger.info("Slettet SED document OK")
                getCounter("SLETTSEDOK").increment()
                return true
            } else {
                throw SedIkkeSlettetException("Feil, SED document ble ikke slettet")
            }
        } catch (sx: SedIkkeSlettetException) {
            logger.error(sx.message)
            getCounter("SLETTSEDFEIL").increment()
            throw SedIkkeSlettetException(sx.message)
        } catch (ex: Exception) {
            logger.error(ex.message)
            getCounter("SLETTSEDFEIL").increment()
            throw EuxServerException(ex.message)
        }
    }

    /**
     * prøver å sende et SED doument på RINA ut til eu/mottaker.
     * @param euxCaseId  er iden til den aktuelle Buc/Rina sak
     * @param documentId er iden til det unike dokuement/Sed som skal sendes.
     * true hvis alt ok, og sed sendt. Exception error hvis feil.
     */
    @Throws(SedDokumentIkkeSendtException::class, EuxServerException::class)
    fun sendDocumentById(euxCaseId: String, documentId: String): Boolean {

        val path = "/buc/{RinaSakId}/sed/{DokumentId}/send"
        val uriParams = mapOf("RinaSakId" to euxCaseId, "DokumentId" to documentId)
        val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)

        return try {
            restTemplateErrorhandler(
                    {
                        euxOidcRestTemplate.exchange(
                                builder.toUriString(),
                                HttpMethod.POST,
                                null,
                                String::class.java)
                    }
                    , euxCaseId
                    ,"sendSED"
                    ,"sending av Sed, "
            )
            true
        } catch (ex: Exception) {
            logger.error(ex.message, ex)
            throw SedDokumentIkkeSendtException("Feil, SED document ble ikke sendt")
        }
    }

    /**
     * List all institutions connected to RINA.
     */
    fun getInstitutions(bucType: String, landkode: String? = ""): List<String> {
        val builder = UriComponentsBuilder.fromPath("/institusjoner")
                .queryParam("BuCType", bucType)
                .queryParam("LandKode", landkode ?: "")

        val responseInstitution = restTemplateErrorhandler(
                {
                    euxOidcRestTemplate.exchange(builder.toUriString(),
                            HttpMethod.GET,
                            null,
                            String::class.java)
                }
                , ""
                , "Institusjoner"
                , "Feil ved innhenting av institusjoner"
        )
        return mapJsonToAny(responseInstitution.body!!, typeRefs())
    }

    fun getRinasaker(fnr: String, rinaSakIder: List<String>): List<Rinasak> {
        logger.debug("Henter opp rinasaker på fnr")

        // Henter rina saker basert på fnr
        val rinaSakerMedFnr = getRinasaker(fnr, null, null, null)

        // Filtrerer vekk saker som allerede er hentet som har fnr
        val rinaSakIderMedFnr = hentRinaSakIder(rinaSakerMedFnr)
        val rinaSakIderUtenFnr = rinaSakIder.minus(rinaSakIderMedFnr)

        // Henter rina saker som ikke har fnr
        val rinaSakerUtenFnr = rinaSakIderUtenFnr.stream().map { getRinasaker(null, it, null, null).first() }.distinct().toList()
        return rinaSakerMedFnr.plus(rinaSakerUtenFnr)
    }

    fun getFilteredArchivedaRinasaker(list: List<Rinasak>): List<String> {
        val gyldigBucs = mutableListOf("H_BUC_07", "R_BUC_01", "R_BUC_02", "M_BUC_02", "M_BUC_03a","M_BUC_03b")
        gyldigBucs.addAll(initSedOnBuc().keys.map { it }.toList())

        return list.filterNot { rinasak -> rinasak.status == "archived" }
                .filter { rinasak -> gyldigBucs.contains(rinasak.processDefinitionId) }
                .sortedBy {  rinasak -> rinasak.id }
                .map { rinasak -> rinasak.id!! }
                .toList()
    }

    /**
     * Lister alle rinasaker på valgt fnr eller euxcaseid, eller bucType...
     * fnr er påkrved resten er fritt
     * @param fnr fødselsnummer
     * @param rinaSakIder rina sak IDer
     */
    fun getRinaSakerFilterKunRinaId(fnr: String, rinaSakIder: List<String>): List<String> {
        logger.debug("Henter opp rinasaker på fnr")

        // Henter rina saker basert på fnr
        val rinaSakerMedFnr = getRinasaker(fnr, null, null, null)

        //filterer kun på ID (euxCaseId)
        val rinaSakIderMedFnr = hentRinaSakIder(rinaSakerMedFnr)
        logger.debug("Rinasaker fra rina: $rinaSakIderMedFnr")

        // Filtrerer vekk saker som allerede er hentet som har fnr
        return rinaSakIder.plus(rinaSakIderMedFnr).distinct()
    }

    /**
     * Returnerer en distinct liste av rinaSakIDer
     *  @param rinaSaker liste av rinasaker fra EUX datamodellen
     */
    private fun hentRinaSakIder(rinaSaker: List<Rinasak>) = rinaSaker.stream().map { it.id!! }.toList()

    //TODO utgåår det er ingen gunn å hente alle buc på godkjente pernsjons-buctype?
    fun getRinasakerPaaBuctype(): List<Rinasak> {
        val rinasaker = mutableListOf<Rinasak>()
        initSedOnBuc().keys.forEach { buctype ->
            logger.debug("Hnter opp rinasaker på buctype: $buctype")
            val result = getRinasaker("", "", buctype, "open")
            logger.debug("Antall rinasaker : ${result.size}")
            result.toCollection(rinasaker)
        }
        logger.debug("Totalt antall rinasaker på buctyper: ${rinasaker.size}")
        return rinasaker.asSequence().sortedByDescending { it.id }.toList()
    }

    /**
     * Lister alle rinasaker på valgt fnr eller euxcaseid, eller bucType...
     * fnr er påkrved resten er fritt
     * @param fnr fødselsnummer
     * @param rinaSakIder rina sak IDer
     */
    fun getRinasaker(fnr: String?, euxCaseId: String?, bucType: String?, status: String?): List<Rinasak> {
        require(!(fnr == null && euxCaseId == null && bucType == null && status == null)) { "Minst et søkekriterie må fylles ut for å få et resultat fra Rinasaker" }

        val uriComponent = UriComponentsBuilder.fromPath("/rinasaker")
                .queryParam("fødselsnummer", fnr ?: "")
                .queryParam("rinasaksnummer", euxCaseId ?: "")
                .queryParam("buctype", bucType ?: "")
                .queryParam("status", status ?: "")
                .build()

        val response = restTemplateErrorhandler(
                {
                    euxOidcRestTemplate.exchange(uriComponent.toUriString(),
                            HttpMethod.GET,
                            null,
                            String::class.java)
                }
                , euxCaseId ?: ""
                    , "hentRinasaker"
                ,"Feil ved Rinasaker"
        )

        return mapJsonToAny(response.body!!, typeRefs())
    }

    fun getSingleBucAndSedView(euxCaseId: String): BucAndSedView {
        return try {
            BucAndSedView.from( getBuc(euxCaseId) )
        } catch (ex: Exception) {
            logger.error("Feiler ved utlevering av enkel bucandsedview ${ex.message}", ex)
            BucAndSedView.fromErr( ex.message )
        }
    }

    fun getBucAndSedView(rinasaker: List<String>): List<BucAndSedView> {
        val startTime = System.currentTimeMillis()
        val list = rinasaker
                .map { rinaid ->
                        try {
                            BucAndSedView.from( getBuc( rinaid ) )
                        } catch (ex: Exception) {
                            logger.error(ex.message, ex)
                            BucAndSedView.fromErr( ex.message )
                        }
                    }
                .toList()

        logger.debug(" ferdig returnerer list av BucAndSedView. Antall BUC: ${list.size}")

        logger.debug(" sortert listen på startDate nyeste dato først")
        val sortlist = list.asSequence().sortedByDescending { it.startDate }.toList()

        logger.debug(" tiden tok ${System.currentTimeMillis() - startTime} ms.")
        return sortlist
    }

    fun createBuc(bucType: String): String {
        val correlationId = UUID.randomUUID().toString()
        val builder = UriComponentsBuilder.fromPath("/buc")
                .queryParam("BuCType", bucType)
                .queryParam("KorrelasjonsId", correlationId)
                .build()

        logger.debug("Kontakter EUX for å prøve på opprette ny BUC med korrelasjonId: $correlationId")
        val response = restTemplateErrorhandler(
            {
                euxOidcRestTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                String::class.java)
            }
            ,bucType
            ,"createBuc"
            ,"Opprett Buc, "
        )
        response.body?.let { return it } ?: { logger.error("Får ikke opprettet BUC på bucType: $bucType")
            throw IkkeFunnetException("Fant ikke noen euxCaseId på bucType: $bucType")}()
    }

    fun convertListInstitusjonItemToString(deltakere: List<InstitusjonItem>): String {
        val encodedList = mutableListOf<String>()
        deltakere.forEach { item ->
            Preconditions.checkArgument(item.institution.contains(":"), "Ikke korrekt format på mottaker/institusjon... ")
            encodedList.add("&mottakere=${item.institution}")
        }
        return encodedList.joinToString(separator = "")
    }

    fun putBucMottakere(euxCaseId: String, deltaker: List<InstitusjonItem>): Boolean {
//        //cpi/buc/245580/mottakere?KorrelasjonsId=23424&mottakere=NO%3ANAVT003&mottakere=NO%3ANAVT008"
        val correlationId = UUID.randomUUID().toString()
        val builder = UriComponentsBuilder.fromPath("/buc/$euxCaseId/mottakere")
                .queryParam("KorrelasjonsId", correlationId)
                .build()
        val url = builder.toUriString() + convertListInstitusjonItemToString(deltaker)

        logger.debug("Kontakter EUX for å legge til deltager: $deltaker med korrelasjonId: $correlationId på type: $euxCaseId")

        val result = restTemplateErrorhandler(
            {
            euxOidcRestTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    null,
                    String::class.java)
            }
        , euxCaseId
        , "putmottaker"
        ,"Feiler ved behandling. Får ikke lagt til mottaker på Buc. "
        )
        return result.statusCode == HttpStatus.OK

    }

    fun leggTilVedleggPaaDokument(aktoerId: String,
                                  rinaSakId: String,
                                  rinaDokumentId: String,
                                  vedlegg: Vedlegg,
                                  filtype: String) {
        try {

            val headers = HttpHeaders()
            headers.contentType = MediaType.MULTIPART_FORM_DATA

            val disposition = ContentDisposition
                    .builder("form-data")
                    .name("file")
                    .filename("")
                    .build().toString()

            val attachmentMeta = LinkedMultiValueMap<String, String>()
            attachmentMeta.add(HttpHeaders.CONTENT_DISPOSITION, disposition)
            val dokumentInnholdBinary = Base64.getDecoder().decode(vedlegg.filInnhold)
            val attachmentPart = HttpEntity(dokumentInnholdBinary, attachmentMeta)

            val body = LinkedMultiValueMap<String, Any>()
            body.add("multipart", attachmentPart)

            val requestEntity = HttpEntity(body, headers)

            val queryUrl = UriComponentsBuilder
                    .fromPath("/buc/")
                    .path(rinaSakId)
                    .path("/sed/")
                    .path(rinaDokumentId)
                    .path("/vedlegg")
                    .queryParam("Filnavn", vedlegg.filnavn.replaceAfterLast(".", "").removeSuffix("."))
                    .queryParam("Filtype", filtype)
                    .queryParam("synkron", true)
                    .build().toUriString()
            logger.info("Legger til vedlegg i buc: $rinaSakId, sed: $rinaDokumentId")

            val response = euxOidcRestTemplate.exchange(
                    queryUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String::class.java)
            if (!response.statusCode.is2xxSuccessful) {
                throw RuntimeException("En feil opppstod under tilknytning av vedlegg: ${response.statusCode}, ${response.body}")
            }
        } catch (ex: java.lang.Exception) {
            logger.error("En feil opppstod under tilknytning av vedlegg, $ex", ex.printStackTrace())
            throw ex
        } finally {
            val file = File(Paths.get("").toAbsolutePath().toString() + "/" + vedlegg.filnavn)
            file.delete()
        }
    }

    //Legger en eller flere deltakere/institusjonItem inn i Rina. (Itererer for hver en)
    fun addDeltagerInstitutions(euxCaseId: String, mottaker: List<InstitusjonItem>): Boolean {
        logger.debug("Prøver å legge til liste over nye InstitusjonItem til Rina ")
        return putBucMottakere(euxCaseId, mottaker)
    }

    //Henter ut Kravtype og Fnr fra P2100 og P15000
    fun hentFnrOgYtelseKravtype(euxCaseId: String, documentId: String): PinOgKrav {
        val sed = getSedOnBucByDocumentId(euxCaseId, documentId)

        //validere om SED er virkelig en P2100 eller P15000
        if (SEDType.P2100.name == sed.sed) {
            return PinOgKrav(
                    fnr = getFnrMedLandkodeNO(sed.pensjon?.gjenlevende?.person?.pin),
                    krav = sed.nav?.krav ?: Krav()
            )
        }
        //P15000 sjekke om det er 02 Gjenlevende eller ikke
        if (SEDType.P15000.name == sed.sed) {
            val krav = sed.nav?.krav ?: Krav()
            return if ("02" == krav.type) {
                PinOgKrav(
                        fnr = getFnrMedLandkodeNO(sed.pensjon?.gjenlevende?.person?.pin),
                        krav = krav
                )
            } else {
                PinOgKrav(
                        fnr = getFnrMedLandkodeNO(sed.nav?.bruker?.person?.pin),
                        krav = sed.nav?.krav ?: Krav()
                )
            }
        }
        throw SedDokumentIkkeGyldigException("SED gyldig SED av type P2100 eller P15000")
    }

    fun getFnrMedLandkodeNO(pinlist: List<PinItem>?): String? {
        pinlist?.forEach {
            if ("NO" == it.land) {
                return it.identifikator
            }
        }
        return null
    }

    @Throws(EuxServerException::class)
    fun pingEux(): Boolean {

        val builder = UriComponentsBuilder.fromPath("/kodeverk")
                .queryParam("Kodeverk", "sedtyper")
                .build()

        val pingResult = restTemplateErrorhandler (
            {
                    euxOidcRestTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    String::class.java)
            }
            ,""
            ,"euxping"
            ,""
        )
        return pingResult.statusCode == HttpStatus.OK
    }

    fun filterUtGyldigSedId(sedJson: String?): List<Pair<String, String>> {
        val validSedtype = listOf("P2000","P2100","P2200","P1000",
                "P5000","P6000","P7000", "P8000",
                "P10000","P1100","P11000","P12000","P14000","P15000")
        val sedRootNode = mapper.readTree(sedJson)
        return sedRootNode
                .filterNot { node -> node.get("status").textValue() =="empty" }
                .filter { node ->  validSedtype.contains(node.get("type").textValue()) }
                .map { node -> Pair(node.get("id").textValue(), node.get("type").textValue()) }
                .sortedBy { (_, sorting) -> sorting }
                .toList()
    }

    /**
     * Own impl. no list from eux that contains list of SED to a speific BUC
     */
    companion object {
        @JvmStatic
        fun initSedOnBuc(): Map<String, List<String>> {
            return mapOf(
                    "P_BUC_01" to listOf("P2000"),
                    "P_BUC_02" to listOf("P2100"),
                    "P_BUC_03" to listOf("P2200"),
                    "P_BUC_05" to listOf("P8000"),
                    "P_BUC_06" to listOf("P5000", "P6000", "P7000", "P10000"),
                    "P_BUC_09" to listOf("P14000"),
                    "P_BUC_10" to listOf("P15000"),
                    "P_BUC_04" to listOf("P1000"),
                    "P_BUC_07" to listOf("P11000"),
                    "P_BUC_08" to listOf("P12000")
            )
        }

        @JvmStatic
        fun getAvailableSedOnBuc(bucType: String?): List<String> {
            val map = initSedOnBuc()

            if (bucType.isNullOrEmpty()) {
                val set = mutableSetOf<String>()
                map["P_BUC_01"]?.let { set.addAll(it) }
                map["P_BUC_02"]?.let { set.addAll(it) }
                map["P_BUC_03"]?.let { set.addAll(it) }
                map["P_BUC_05"]?.let { set.addAll(it) }
                map["P_BUC_06"]?.let { set.addAll(it) }
                map["P_BUC_09"]?.let { set.addAll(it) }
                map["P_BUC_10"]?.let { set.addAll(it) }
                return set.toList()
            }
            return map[bucType].orEmpty()
        }

        @JvmStatic
        fun getValidBucAndSeds(bucType: String?) {

        }
    }

    fun <T> restTemplateErrorhandler(restTemplateFunction: () -> ResponseEntity<T>, euxCaseId: String, metricName: String, prefixErrorMessage: String): ResponseEntity<T> {
        return metricsHelper.measure(metricName) {
            return@measure try {
                    val response = restTemplateFunction.invoke()
                    response
            } catch (hcee: HttpClientErrorException) {
                val errorBody = hcee.responseBodyAsString
                logger.error("$prefixErrorMessage, HttpClientError med euxCaseID: $euxCaseId, body: $errorBody")
                when (hcee.statusCode) {
                    HttpStatus.UNAUTHORIZED -> throw RinaIkkeAutorisertBrukerException("Authorization token required for Rina,")
                    HttpStatus.FORBIDDEN -> throw ForbiddenException("Forbidden, Ikke tilgang")
                    HttpStatus.NOT_FOUND -> throw IkkeFunnetException("Ikke funnet")
                    else -> throw GenericUnprocessableEntity("Uoppdaget feil har oppstått!!, $errorBody")
                }
            } catch (hsee: HttpServerErrorException) {
                val errorBody = hsee.responseBodyAsString
                logger.error("$prefixErrorMessage, HttpServerError med euxCaseID: $euxCaseId, feilkode body: $errorBody", hsee)
                when (hsee.statusCode) {
                    HttpStatus.INTERNAL_SERVER_ERROR -> throw EuxRinaServerException("Rina serverfeil, kan også skyldes ugyldig input, $errorBody")
                    HttpStatus.GATEWAY_TIMEOUT -> throw GatewayTimeoutException("Venting på respons fra Rina resulterte i en timeout, $errorBody")
                    else -> throw GenericUnprocessableEntity("Uoppdaget feil har oppstått!!, $errorBody")
                }
            } catch (uhsce: UnknownHttpStatusCodeException) {
                val errorBody = uhsce.responseBodyAsString
                logger.error("$prefixErrorMessage, med euxCaseID: $euxCaseId errmessage: $errorBody", uhsce)
                throw GenericUnprocessableEntity("Ukjent statusefeil, $errorBody")
            } catch (ex: Exception) {
                logger.error("$prefixErrorMessage, med euxCaseID: $euxCaseId", ex)
                throw ServerException("Ukjent Feil oppstod euxCaseId: $euxCaseId,  ${ex.message}")
            }
        }
    }

}


//--- Disse er benyttet av restTemplateErrorhandler  -- start
@ResponseStatus(value = HttpStatus.NOT_FOUND)
class IkkeFunnetException(message: String) : RuntimeException(message)

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
class RinaIkkeAutorisertBrukerException(message: String?) : RuntimeException(message)

@ResponseStatus(value = HttpStatus.FORBIDDEN)
class ForbiddenException(message: String?) : RuntimeException(message)

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class UgyldigCaseIdException(message: String) : RuntimeException(message)

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class EuxRinaServerException(message: String?) : RuntimeException(message)

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
class GenericUnprocessableEntity(message: String) : RuntimeException(message)

@ResponseStatus(value = HttpStatus.GATEWAY_TIMEOUT)
class GatewayTimeoutException(message: String?) : RuntimeException(message)

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
class ServerException(message: String?): RuntimeException(message)

//--- Disse er benyttet av restTemplateErrorhandler  -- slutt


@ResponseStatus(value = HttpStatus.NOT_FOUND)
class SedDokumentIkkeOpprettetException(message: String) : Exception(message)

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class SedDokumentIkkeLestException(message: String?) : Exception(message)

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class SedIkkeSlettetException(message: String?) : Exception(message)

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class BucIkkeMottattException(message: String?) : Exception(message)

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
class EuxGenericServerException(message: String?) : Exception(message)

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
class SedDokumentIkkeSendtException(message: String?) : Exception(message)

@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
class EuxServerException(message: String?) : Exception(message)

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class SedDokumentIkkeGyldigException(message: String?) : Exception(message)

