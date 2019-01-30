package no.nav.eessi.eessifagmodul.services.eux

import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.services.eux.bucmodel.Buc
import no.nav.eessi.eessifagmodul.utils.*
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Description
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException
import java.net.UnknownHostException
import java.util.*
import javax.naming.ServiceUnavailableException


@Service
@Description("Service class for EuxBasis - EuxCpiServiceController.java")
class EuxService(private val euxOidcRestTemplate: RestTemplate) {
    private val logger = LoggerFactory.getLogger(EuxService::class.java)

    // Nye API kall er er fra 23.01.19
    // https://eux-app.nais.preprod.local/swagger-ui.html#/eux-cpi-service-controller/getDocumentUsingGET


    //Oppretter ny RINA sak(buc) og en ny Sed
    @Throws(EuxServerException::class, RinaCasenrIkkeMottattException::class)
    fun opprettBucSed(navSED: SED, bucType: String, mottakerid: String, fagSaknr: String): String {
        logger.info("opprettBucSed navSED: $navSED, bucType: $bucType, mottakerid: $mottakerid, fagSaknr: $fagSaknr")
        val path = "/buc/sed"
        val builder = UriComponentsBuilder.fromPath(path)
                .queryParam("BucType", bucType)
                .queryParam("MottakerId", bucType)
                .queryParam("FagSakNummer", fagSaknr)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val httpEntity = HttpEntity(navSED.toJson(), headers)

        try {
            logger.info("Prøver å kontakte EUX /${builder.toUriString()}")
            val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
            if (response.statusCode.is2xxSuccessful) {
                getCounter("OPPRETTBUCOGSEDOK").increment()
                return response.body!!
            } else {
                throw RinaCasenrIkkeMottattException("Ikke mottatt RINA casenr, feiler ved opprettelse av BUC og SED")
            }
        } catch (rx: RinaCasenrIkkeMottattException) {
            logger.error(rx.message)
            getCounter("OPPRETTBUCOGSEDFEIL").increment()
            throw RinaCasenrIkkeMottattException(rx.message)
        } catch (ex: Exception) {
            logger.error(ex.message)
            getCounter("OPPRETTBUCOGSEDFEIL").increment()
            throw EuxServerException("Feiler ved kontakt mot EUX")
        }
    }


    //ny SED på ekisterende buc
    @Throws(EuxServerException::class, SedDokumentIkkeOpprettetException::class)
    fun opprettSedOnBuc(navSED: SED, euxCaseId: String): Boolean {
        val path = "/buc/{RinaSakId}/sed"

        val uriParams = mapOf("RinaSakId" to euxCaseId)
        val builder = UriComponentsBuilder.fromUriString(path)
                .queryParam("KorrelasjonsId", UUID.randomUUID().toString())
                .buildAndExpand(uriParams)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val httpEntity = HttpEntity(navSED.toJson(), headers)

        try {
            logger.info("Prøver å kontakte EUX /${builder.toUriString()}")
            val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
            return if (response.statusCode.is2xxSuccessful) {
                getCounter("OPPRETTBUCOGSEDOK").increment()
                true
            } else {
                throw SedDokumentIkkeOpprettetException("Nav Sed ikke opprettet")
            }
        } catch (sx: SedDokumentIkkeOpprettetException) {
            logger.error(sx.message)
            getCounter("OPPRETTBUCOGSEDFEIL").increment()
            throw SedDokumentIkkeOpprettetException(sx.message!!)
        } catch (ex: Exception) {
            logger.error(ex.message)
            getCounter("OPPRETTBUCOGSEDFEIL").increment()
            throw EuxServerException("Feiler ved kontakt mot EUX")
        }
    }


    //henter ut sed fra rina med bucid og documentid
    fun getSedOnBuc(euxCaseId: String, sedType: SEDType): List<SED> {
        val docid = getBucUtils(euxCaseId).getDocuments()

        val sedlist = mutableListOf<SED>()
        docid.forEach {
            it.id?.let {
                sedlist.add(getSedOnBucByDocumentId(euxCaseId, it))
            }
        }
        return sedlist
    }


    //henter ut sed fra rina med bucid og documentid
    @Throws(EuxServerException::class, SedIkkeMottattException::class)
    fun getSedOnBucByDocumentId(euxCaseId: String, documentId: String): SED {
        val path = "/buc/{RinaSakId}/sed/{DokumentId}"
        val uriParams = mapOf("RinaSakId" to euxCaseId, "DokumentId" to documentId)
        val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)

        try {
            logger.info("Prøver å kontakte EUX /${builder.toUriString()}")
            val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, null, String::class.java)
            val jsonbuc = response.body ?: throw SedIkkeMottattException("Ikke mottatt SED, feiler ved uthenting av Nav-Sed")
            getCounter("HENTSEDOK").increment()
            return mapJsonToAny(jsonbuc, typeRefs())
        } catch (rx: SedIkkeMottattException) {
            logger.error(rx.message)
            getCounter("HENTSEDFEIL").increment()
            throw SedIkkeMottattException(rx.message)
        } catch (ex: Exception) {
            logger.error(ex.message)
            getCounter("HENTSEDFEIL").increment()
            throw EuxServerException("Feiler ved kontakt mot EUX")
        }

    }


    //henter ut bucdata fra valgt buc/euxCaseId
    @Throws(BucIkkeMottattException::class, EuxServerException::class)
    fun getBuc(euxCaseId: String): Buc {
        val path = "/buc/{RinaSakId}"
        val uriParams = mapOf("RinaSakId" to euxCaseId)
        val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)

        try {
            logger.info("Prøver å kontakte EUX /${builder.toUriString()}")
            val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, null, String::class.java)
            if (response.statusCode.is2xxSuccessful) {
                val jsonbuc = response.body!!
                getCounter("HENTBUCOK").increment()
                return mapJsonToAny(jsonbuc, typeRefs())
            } else {
                throw BucIkkeMottattException("Ikke mottatt Buc, feiler ved uthenting av Buc")
            }
        } catch (rx: BucIkkeMottattException) {
            logger.error(rx.message)
            getCounter("HENTBUCFEIL").increment()
            throw BucIkkeMottattException(rx.message)
        } catch (ex: Exception) {
            logger.error(ex.message)
            getCounter("HENTBUCFEIL").increment()
            throw EuxServerException("Feiler ved kontakt mot EUX")
        }

    }

    //henter ut BucUtils med valgt Buc for diverse uthentinger..
    fun getBucUtils(euxCaseId: String): BucUtils {
        return BucUtils(getBuc(euxCaseId))
    }


    //skal denne være her? eller bær en benytte BucUtils direkte?
    //hjelpefunkson for å hente ut list over alle documentid til valgt SEDType (kan ha flere docid i buc)
    fun findDocmentIdBySedType(euxCaseId: String, sedType: SEDType): List<String?> {
        val doclist = getBucUtils(euxCaseId).findAndFilterDocumentItemByType(sedType)
        return doclist.map { it.id }.toList()
    }


















    //Eldre API kall er under denne disse vil ikke virke

    //Henter en liste over tilgjengelige aksjoner for den aktuelle RINA saken PK-51365"
    fun getPossibleActions(euSaksnr: String): List<RINAaksjoner> {
        val builder = UriComponentsBuilder.fromPath("/MuligeAksjoner")
                .queryParam("RINASaksnummer", euSaksnr)

        val httpEntity = HttpEntity("")

        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, typeRef<String>())
        val responseBody = response.body!!
        try {
            if (response.statusCode.isError) {
                getCounter("AKSJONFEIL").increment()
                throw createErrorMessage(responseBody)
            } else {
                getCounter("AKSJONOK").increment()
                return mapJsonToAny(responseBody, typeRefs())
            }
        } catch (ex: IOException) {
            getCounter("AKSJONFEIL").increment()
            throw RuntimeException(ex.message)
        }
    }

    /*
        hjelpe funksjon for sendSED må hente ut dokumentID for valgt sed f.eks P2000
     */
    fun hentDocuemntID(euxCaseId: String, sed: String): String {
        val aksjon = "Send"
        val aksjoner = getPossibleActions(euxCaseId)
        aksjoner.forEach {
            if (sed == it.dokumentType && aksjon == it.navn) {
                return it.dokumentId ?: throw IkkeGyldigKallException("Ingen gyldig dokumentID funnet")
            }
        }
        throw IkkeGyldigKallException("Ingen gyldig dokumentID funnet")
    }

    //TODO: euxBasis hva finnes av metoder for:
    //TODO: euxBasis metode for å legge til flere mottakere (Institusjoner)
    //TODO: euxBasis metode for å fjenre en eller flere mottakere (Institusjoner)

    /**
     * call to send sed on rina document.
     *
     * @parem euxCaseID (rinaid)
     * @param korrelasjonID CorrelationId
     * @param sed (sed type vedtak, P2000)
     */
    fun sendSED(euxCaseId: String, sed: String, korrelasjonID: String): Boolean {
        val documentID = hentDocuemntID(euxCaseId, sed)
        if (documentID.isBlank()) {
            throw IkkeGyldigKallException("Kan ikke Sende valgt sed")
        }

        val builder = UriComponentsBuilder.fromPath("/SendSED")
                .queryParam("RINASaksnummer", euxCaseId)
                .queryParam("DokumentID", documentID)
                .queryParam("KorrelasjonsID", korrelasjonID)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val httpEntity = HttpEntity("", headers)

        logger.info("sendSED KorrelasjonsID : {}", korrelasjonID)
        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
        logger.info("Response SendSED på Rina: $euxCaseId, response:  $response")

        if (response.statusCodeValue == 200) {
            getCounter("SENDSEDOK").increment()
            return true
        }
        getCounter("SENDSEDFEIL").increment()
        return false
    }


    /**
     * call to make new sed on existing rina document.
     *
     * @param sed (actual SED)
     * @parem euxCaseID (rina id)
     * @param korrelasjonID CorrelationId
     */
    //void no confirmaton?
    @Throws(UnknownHostException::class)
    fun createSEDonExistingRinaCase(sed: SED, euxCaseId: String, korrelasjonID: String): HttpStatus {

        val builder = UriComponentsBuilder.fromPath("/SED")
                .queryParam("RINASaksnummer", euxCaseId)
                .queryParam("Sed", sed.sed)
                .queryParam("KorrelasjonsID", korrelasjonID)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val httpEntity = HttpEntity(sed.toJson(), headers)

        logger.info("createSEDonExistingRinaCase KorrelasjonsID : {}", korrelasjonID)
        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)

        if(response.statusCode.is2xxSuccessful) {
            logger.info("Response opprett SED på Rina: $euxCaseId, response:  $response")
            getCounter("OPPRETTEDOK").increment()
        }
        logger.error("Opprettelse av SED på Rina feilet")
        getCounter("OPPRETTEDFEIL").increment()

        return response.statusCode
    }

    /**
     * call to fetch existing sed document on existing rina case.
     * @param euxCaseId (rina id)
     * @param documentId (sed documentid)
     */
    fun fetchSEDfromExistingRinaCase(euxCaseId: String, documentId: String): SED {

        val builder = UriComponentsBuilder.fromPath("/SED")
                .queryParam("RINASaksnummer", euxCaseId)
                .queryParam("DokumentID", documentId)

        //cpi/buc/2342342/sed/3545345
        val httpEntity = HttpEntity("")

        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, typeRef<String>())
        val responseBody = response.body ?: throw SedDokumentIkkeOpprettetException("Sed dokument ikke funnet")
        try {
            if (response.statusCode.isError) {
                getCounter("HENTSEDFEIL").increment()
                throw SedDokumentIkkeLestException("Får ikke lest SED dokument fra Rina")
            } else {
                getCounter("HENTSEDOK").increment()
                return SED.fromJson(responseBody) //  mapJsonToAny(responseBody, typeRefs())
            }
        } catch (ex: Exception) {
            getCounter("HENTSEDFEIL").increment()
            throw RuntimeException(ex.message)
        }
    }

    /**
     * call to delete existing sed document on existing rina case.
     * @param euxCaseId (rina id)
     * @param documentId (sed documentid)
     */
    fun deleteSEDfromExistingRinaCase(euxCaseId: String, documentId: String) {
        val builder = UriComponentsBuilder.fromPath("/SED")
                .queryParam("RINASaksnummer", euxCaseId)
                .queryParam("DokumentID", documentId)

        val httpEntity = HttpEntity("")
        try {
            val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.DELETE, httpEntity, typeRef<String>()).statusCode
            if(response.is2xxSuccessful){
                getCounter("SLETTSEDOK").increment()
            }
        } catch (ex: IOException) {
            getCounter("SLETTSEDFEIL").increment()
            throw RuntimeException(ex.message)
        }
    }


    /**
     * Call the orchestrator endpoint with necessary information to create a case in RINA, set
     * its receiver, create a document and add attachments to it.
     *
     * The method is asynchronous and simply returns the new case ID after creating the case. The rest
     * of the logic is executed afterwards.
     *
     * if something goes wrong after caseid. no sed is shown on case.
     *
     * @param sed SED-document in NAV-format
     * @param bucType The RINA case type to create
     * @param fagSaknr local case number
     * @param mottaker The RINA ID of the organisation that is to receive the SED on a sned action
     * @param vedleggType File type of attachments
     * @param korrelasjonID CorrelationId
     * @return The ID of the created case
     */
    fun createCaseAndDocument(sed: SED, bucType: String, fagSaknr: String, mottaker: String, vedleggType: String = "", korrelasjonID: String): String {

        val builder = UriComponentsBuilder.fromPath("/OpprettBuCogSED")
                .queryParam("BuCType", bucType)
                .queryParam("FagSakNummer", fagSaknr)
                .queryParam("MottakerID", mottaker)
                .queryParam("Filtype", vedleggType)
                .queryParam("KorrelasjonsID", korrelasjonID)

        val map: MultiValueMap<String, Any> = LinkedMultiValueMap()
        val document = getFileAsResource(sed.toJson().toByteArray(), "document")
//        val document = object : ByteArrayResource(sed.toJson().toByteArray()) {
//            override fun getFilename(): String? {
//                return "document"
//            }
//        }
        map.add("document", document)
        map.add("attachment", null)

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val httpEntity = HttpEntity(map, headers)

        try {
            logger.info("Prøver å kontakte EUX /OpprettBuCogSED med KorrelasjonsID : {}", korrelasjonID)
            val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
            val euxCaseId = response.body
                    ?: throw RinaCasenrIkkeMottattException("Ikke mottatt RINA casenr, feiler ved opprettelse av BUC og SED")
            getCounter("OPPRETTBUCOGSEDOK").increment()
            return euxCaseId
        } catch (ex: Exception) {
            logger.error(ex.message)
            getCounter("OPPRETTBUCOGSEDFEIL").increment()
            throw SedDokumentIkkeOpprettetException("Feil ved kontakt mot EUX/RINA")
        }
    }

    /**
     *  An simplified interface for creating a case with an initial document without attachment
     */
    @Deprecated(message = "Søttes ikke lenger av aux", replaceWith = ReplaceWith("createCaseAndDocument"))
    fun createCaseWithDocument(sed: SED, bucType: String, mottaker: String): String {

        val builder = UriComponentsBuilder.fromPath("/CreateCaseWithDocument")
                .queryParam("BuCType", bucType)
                .queryParam("MottakerID", mottaker)

        val document = object : ByteArrayResource(sed.toJson().toByteArray()) {
            override fun getFilename(): String? {
                return "document"
            }
        }

        val map: MultiValueMap<String, Any> = LinkedMultiValueMap()
        map.add("document", document.byteArray)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PROBLEM_JSON
        val httpEntity = HttpEntity(map, headers)

        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
        return response.body
                ?: throw RinaCasenrIkkeMottattException("Ikke mottatt RINA casenr, feiler ved opprettelse av BUC og SED")

    }

    /**
     * Call to EUX-app to get list over mal/template for selected Bucid
     */
    fun getAvailableDocumentTemplate(euxCaseId: String, sedType: SEDType?): String {
        val builder = UriComponentsBuilder.fromPath("/TilgjengeligDokumentMal")
                .queryParam("RINASaksnummer", euxCaseId)
                .queryParam("SEDType", sedType ?: "")

        val httpEntity = HttpEntity("")
        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, typeRef<String>())
        return response.body ?: throw ServiceUnavailableException("Mangler svar fra EUX")
    }

    /**
     * List all institutions connected to RINA.
     * (all take long time, country faster)
     */
    fun getInstitutions(landkode: String? = ""): List<String> {
        val builder = UriComponentsBuilder.fromPath("/Institusjoner")
                .queryParam("LandKode", landkode ?: "")

        val httpEntity = HttpEntity("")
        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, typeRef<List<String>>())
        return response.body ?: listOf()
    }
}
