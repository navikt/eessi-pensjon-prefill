package no.nav.eessi.eessifagmodul.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.eessifagmodul.models.RINASaker
import no.nav.eessi.eessifagmodul.models.RINAaksjoner
import no.nav.eessi.eessifagmodul.models.RinaCasenrIkkeMottattException
import no.nav.eessi.eessifagmodul.utils.createErrorMessage
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRef
import no.nav.eessi.eessifagmodul.utils.typeRefs
import org.slf4j.Logger
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


@Service
@Description("Service class for EuxBasis - EuxCpiServiceController.java")
class EuxService(private val oidcRestTemplate: RestTemplate) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EuxService::class.java) }

    private val EUX_PATH: String = "/cpi"

    //test mock only
    var overrideheaders: Boolean? = null

    //henter ut status på rina.
    fun getRinaSaker(bucType: String = "",rinaNummer: String? = "", pinID: String? = ""): List<RINASaker> {
        val urlPath = "/RINASaker"

        val builder = UriComponentsBuilder.fromPath("$EUX_PATH$urlPath")
                .queryParam("BuCType", bucType)
                .queryParam("Fødselsnummer", pinID)
                .queryParam("RINASaksnummer", rinaNummer)

        val headers = logonBasis()

        val httpEntity = HttpEntity("", headers)
        val response = oidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, String::class.java)
        val responseBody = response.body!!
        try {
            if (response.statusCode.isError) {
                throw createErrorMessage(responseBody)
            } else {
                val response = mapJsonToAny(responseBody, typeRefs<List<RINASaker>>())
                return response
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex.message)
        }
    }

    //Henter en liste over tilgjengelige aksjoner for den aktuelle RINA saken PK-51365"
    fun getMuligeAksjoner(euSaksnr: String): List<RINAaksjoner> {
        val urlPath = "/MuligeAksjoner"

        val builder = UriComponentsBuilder.fromPath("$EUX_PATH$urlPath")
                .queryParam("RINASaksnummer", euSaksnr)

        val headers = logonBasis()
        val httpEntity = HttpEntity("", headers)

        val response = oidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, typeRef<String>())
        val responseBody = response.body!!
        try {
            if (response.statusCode.isError) {
                throw createErrorMessage(responseBody)
            } else {
                val list = mapJsonToAny(responseBody, typeRefs<List<RINAaksjoner>>()  )
                return list
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex.message)
        }
    }


    /**
     * call to make new sed on existing rina document.
     *
     * @parem euxCaseID (rina id)
     * @param korrelasjonID CorrelationId
     * @param jsonPayLoad (actual sed as json)
     */
    //void no confirmaton?
    fun createSEDonExistingDocument(jsonPayload: String, euxCaseId: String, korrelasjonID: String): HttpStatus {
        val urlPath = "/SED"

        val builder = UriComponentsBuilder.fromPath("$EUX_PATH$urlPath")
            .queryParam("RINASaksnummer", euxCaseId)
            .queryParam("KorrelasjonsID", korrelasjonID)

        val headers = logonBasis()
        headers.contentType = MediaType.APPLICATION_JSON

        val httpEntity = HttpEntity(jsonPayload, headers)
        val response = oidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
        logger.debug("Response opprett SED på Rina: $euxCaseId, response:  $response")
        return response.statusCode
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
     * @param jsonPayload SED-document in NAV-format
     * @param bucType The RINA case type to create
     * @param fagSaknr local case number
     * @param mottaker The RINA ID of the organisation that is to receive the SED on a sned action
     * @param vedleggType File type of attachments
     * @param korrelasjonID CorrelationId
     * @return The ID of the created case
     */
    fun createCaseAndDocument(jsonPayload: String, bucType: String, fagSaknr: String, mottaker: String, vedleggType: String = "", korrelasjonID: String): String {
        val urlPath = "/OpprettBuCogSED"

        val builder = UriComponentsBuilder.fromPath("$EUX_PATH$urlPath")
                .queryParam("BuCType", bucType)
                .queryParam("FagSakNummer", fagSaknr)
                .queryParam("MottagerID", mottaker)
                .queryParam("Filtype", vedleggType)
                .queryParam("KorrelasjonsID", korrelasjonID)

        val map: MultiValueMap<String, Any> = LinkedMultiValueMap()
        val document = object : ByteArrayResource(jsonPayload.toByteArray()) {
            override fun getFilename(): String? {
                return "document"
            }
        }
        map.add("document", document)
        map.add("attachment", null)

        val headers = logonBasis()
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val httpEntity = HttpEntity(map, headers)
        val response = oidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
        return response.body ?: throw RinaCasenrIkkeMottattException("Ikke mottatt RINA casenr, feiler ved opprettelse av BUC og SED")
    }

    //temp fuction to log system onto eux basis
    fun logonBasis(): HttpHeaders {
        logger.debug("overrideheaders : $overrideheaders")
        if (overrideheaders !== null && overrideheaders!! == true) {
            return HttpHeaders()
        }
        val urlPath = "/login"
        val builder = UriComponentsBuilder.fromPath("$EUX_PATH$urlPath")
                .queryParam("username", "T102")
                .queryParam("password", "rina@nav")
//                .queryParam("username", "srvPensjon")
//                .queryParam("password", "Ash5SoxP")

        val httpEntity = HttpEntity("", HttpHeaders())
        val response = oidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
        val header = response.headers

        val cookies = HttpHeaders()
        cookies.set("Cookie", header.getFirst(HttpHeaders.SET_COOKIE))
        logger.debug("setting request cookie : ${header.getFirst(HttpHeaders.SET_COOKIE)}")
        cookies.set("X-XSRF-TOKEN", header.getFirst("X-XSRF-TOKEN"))
        logger.debug("setting request X-XSRF-TOKEN : ${header.getFirst("X-XSRF-TOKEN")}")
        return cookies
    }

}
