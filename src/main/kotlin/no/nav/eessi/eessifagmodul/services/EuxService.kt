package no.nav.eessi.eessifagmodul.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.eessifagmodul.utils.createErrorMessage
import no.nav.eessi.eessifagmodul.utils.typeRef
import no.nav.eessi.eessifagmodul.utils.typeRefs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Description
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException


@Service
@Description("Service class for EuxBasis - EuxCpiServiceController.java")
class EuxService(val oidcRestTemplate: RestTemplate) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EuxService::class.java) }

    private val EUX_PATH: String = "/cpi"

    private val objectMapper = jacksonObjectMapper()

    fun getBuC(euSaksnr: String): String? {
//        val urlpath = "/BuC"
//        val data = "?RINASaksnummer=$euSaksnr"
//        return oidcRestTemplate.getForEntity<String>("$EUX_PATH$urlpath$data", typeRef<String>()).body
        throw NotImplementedError("getBuC not yet implemented")
    }

    fun getBuCtypePerSektor(): List<String> {
//        val urlPath = "/BuCTypePerSektor"
//        return oidcRestTemplate.getForEntity<String>("$EUX_PATH$urlPath", typeRef<String>())
        throw NotImplementedError("getBuCtypePerSektor not yet implemented")
    }

    fun sendSED(euSaksnr: String, korrelasjonID: String, dokumentID: String): Boolean {
//        val urlPath = "/SendSED"
//        val data = "?RINASaksnummer=$euSaksnr&KorrelasjonsID=$korrelasjonID"
//        return oidcRestTemplate.postForEntity<String>("$EUX_PATH$urlPath$data", typeRef<String>())
        throw NotImplementedError("sendSED not yet implemented")
    }

    fun saveDocument(euSaksnr: String, korrelasjonID: String) {
//        val data = "?RINASaksnummer=$euSaksnr&KorrelasjonsID=$korrelasjonID"
//        val pathurl = "/SED"
//        val response = oidcRestTemplate.postForEntity<String>("$EUX_PATH$pathurl$data", typeRef<Unit>())
        throw NotImplementedError("saveDocument not yet implemented")
    }

    fun getDocument(euSaksnr: String, dokumentID: String): String {
//        val data = "?RINASaksnummer=$euSaksnr&DokumentID=$dokumentID"
//        val pathurl = "/SED"
//        val response = oidcRestTemplate.getForEntity<String>("$EUX_PATH$pathurl$data", typeRef<String>())
        throw NotImplementedError("getDocument not yet implemented")
    }

    fun updateDocument(euSaksnr: String, korrelasjonID: String, SEDType: String, dokumentID: String): String? {
//        val data = "?RINASaksnummer=$euSaksnr&KorrelasjonsID=$korrelasjonID"
//        val pathurl = "/SED"
//        val response = oidcRestTemplate.postForEntity<String>("$EUX_PATH$pathurl$data", typeRef<Unit>())
//        return response.body
        throw NotImplementedError("updateDocument not yet implemented")
    }


    //Henter en liste over tilgjengelige aksjoner for den aktuelle RINA saken PK-51365"
    fun getMuligeAksjoner(euSaksnr: String): List<String> {
        val urlPath = "/MuligeAksjoner"

        var data = ""
        if (euSaksnr.isNotBlank()) {
            data = "?RINASaksnummer=$euSaksnr"
        }

        val response = oidcRestTemplate.getForEntity<String>("$EUX_PATH$urlPath$data", typeRef<String>())
        val responseBody = response.body!!
        try {
            if (response.statusCode.isError) {
                throw createErrorMessage(responseBody)
            } else {
                return objectMapper.readValue(responseBody, typeRefs<List<String>>())
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex.message)
        }
    }

    //PK-51002 --
    //Henter ut en liste over registrerte institusjoner innenfor spesifiserte EU-land. PK-51002"
    fun getInstitusjoner(bucType: String, landKode: String): List<String> {
        val urlPath = "/Institusjoner"

        //val data = "?BuCType=$bucType&LandKode=$landKode"
        var data = ""
        if (bucType.isNotBlank() && landKode.isBlank()) {
            data = "?BuCType=$bucType"
        } else if (bucType.isBlank() && landKode.isNotBlank()) {
            data = "?LandKode=$landKode"
        } else if (bucType.isNotBlank() && landKode.isNotBlank()) {
            data = "?BuCType=$bucType&LandKode=$landKode"
        }

        val response = oidcRestTemplate.getForEntity<String>("$EUX_PATH$urlPath$data", typeRef<String>())
        val responseBody = response.body!!
        try {
            if (response.statusCode.isError) {
                throw createErrorMessage(responseBody)
            } else {
                return objectMapper.readValue(responseBody, typeRefs<List<String>>())
            }
        } catch (ex: IOException) {
            throw RuntimeException()
        }
    }

    //Henter ut en liste over alle SED typer som kan opprettes i sakens nåværende tilstand.
    fun getAvailableSEDTypes(euSaksnr: String): List<String> {
//        val urlPath = "/TilgjengeligeSEDTyper"
//        val data = "?RINASaksnummer=$euSaksnr"
//        val response = oidcRestTemplate.getForEntity<String>("$EUX_PATH$urlPath$data", typeRef<String>())
        throw NotImplementedError()
    }

    /**
     * Call the orchestrator endpoint with necessary information to create a case in RINA, set
     * its receiver, create a document and add attachments to it.
     *
     * The method is asynchronous and simply returns the new case ID after creating the case. The rest
     * of the logic is executed afterwards.
     *
     * @param jsonPayload SED-document in NAV-format
     * @param bucType The RINA case type to create
     * @param fagSaknr local case number
     * @param mottaker The RINA ID of the organisation that is to receive the SED on a sned action
     * @param vedleggType File type of attachments
     * @param korrelasjonID CorrelationId
     * @return The ID of the created case
     */
    fun createCaseAndDocument(jsonPayload: String, bucType: String, fagSaknr: String, mottaker: String, vedleggType: String = "", korrelasjonID: String): String? {
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

        val headers = HttpHeaders()
        headers.set("Cookie", "JSESSIONID=51A1D0F7796572408C819B07D20C7928.E34APVW008")
        headers.set("X-XSRF-TOKEN", "49ef2474-263e-4476-8bf1-9dccc9496210")

        headers.contentType = MediaType.MULTIPART_FORM_DATA
        val httpEntity = HttpEntity(map, headers)

        //val response = oidcRestTemplate.exchange("$EUX_PATH$urlPath$data", HttpMethod.POST, httpEntity, String::class.java)
        val response = oidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
        return response.body
    }
}
