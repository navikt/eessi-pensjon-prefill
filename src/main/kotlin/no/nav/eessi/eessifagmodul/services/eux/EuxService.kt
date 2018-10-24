package no.nav.eessi.eessifagmodul.services.eux

import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.utils.createErrorMessage
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRef
import no.nav.eessi.eessifagmodul.utils.typeRefs
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

private val logger = LoggerFactory.getLogger(EuxService::class.java)

@Service
@Description("Service class for EuxBasis - EuxCpiServiceController.java")
class EuxService(private val euxOidcRestTemplate: RestTemplate) {

    //Henter en liste over tilgjengelige aksjoner for den aktuelle RINA saken PK-51365"
    fun getPossibleActions(euSaksnr: String): List<RINAaksjoner> {
        val builder = UriComponentsBuilder.fromPath("/MuligeAksjoner")
                .queryParam("RINASaksnummer", euSaksnr)

        val httpEntity = HttpEntity("")

        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, typeRef<String>())
        val responseBody = response.body!!
        try {
            if (response.statusCode.isError) {
                throw createErrorMessage(responseBody)
            } else {
                return mapJsonToAny(responseBody, typeRefs())
            }
        } catch (ex: IOException) {
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
        logger.debug("Response SendSED på Rina: $euxCaseId, response:  $response")

        if (response.statusCodeValue == 200) {
            return true
        }
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
                .queryParam("KorrelasjonsID", korrelasjonID)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val httpEntity = HttpEntity(sed.toJson(), headers)

        logger.info("createSEDonExistingRinaCase KorrelasjonsID : {}", korrelasjonID)
        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
        logger.debug("Response opprett SED på Rina: $euxCaseId, response:  $response")
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

        val httpEntity = HttpEntity("")

        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, typeRef<String>())
        val responseBody = response.body ?: throw SedDokumentIkkeOpprettetException("Sed dokument ikke funnet")
        try {
            if (response.statusCode.isError) {
                throw SedDokumentIkkeLestException("Får ikke lest SED dokument fra Rina")
            } else {
                return SED.fromJson(responseBody) //  mapJsonToAny(responseBody, typeRefs())
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex.message)
        }
    }

    /**
     * call to fetch existing sed document on existing rina case.
     * @param euxCaseId (rina id)
     * @param documentId (sed documentid)
     */
    fun deleteSEDfromExistingRinaCase(euxCaseId: String, documentId: String): HttpStatus {
        val builder = UriComponentsBuilder.fromPath("/SED")
                .queryParam("RINASaksnummer", euxCaseId)
                .queryParam("DokumentID", documentId)

        val httpEntity = HttpEntity("")
        return euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.DELETE, httpEntity, typeRef<String>()).statusCode

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
        val document = object : ByteArrayResource(sed.toJson().toByteArray()) {
            override fun getFilename(): String? {
                return "document"
            }
        }
        map.add("document", document)
        map.add("attachment", null)

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val httpEntity = HttpEntity(map, headers)


        logger.info("createCaseAndDocument KorrelasjonsID : {}", korrelasjonID)
        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
        return response.body ?: throw RinaCasenrIkkeMottattException("Ikke mottatt RINA casenr, feiler ved opprettelse av BUC og SED")
    }
}
