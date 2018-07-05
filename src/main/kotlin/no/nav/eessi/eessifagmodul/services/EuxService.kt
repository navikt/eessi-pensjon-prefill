package no.nav.eessi.eessifagmodul.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import no.nav.eessi.eessifagmodul.models.RINASaker
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
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException
import java.util.concurrent.TimeUnit


@Service
@Description("Service class for EuxBasis - EuxCpiServiceController.java")
class EuxService(val oidcRestTemplate: RestTemplate) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EuxService::class.java) }

    private val EUX_PATH: String = "/cpi"

    private val objectMapper = jacksonObjectMapper()

    private val buccache = CacheBuilder.newBuilder()
            .expireAfterAccess(2, TimeUnit.HOURS)
            .recordStats()
            .build(object : CacheLoader<Any, List<String>>() {
                @Throws(IOException::class)
                override fun load(s: Any): List<String> {
                    return getBuCtypePerSektor()
                }
            })

    private val instituitioncache = CacheBuilder.newBuilder()
            .expireAfterAccess(2, TimeUnit.HOURS)
            .recordStats()
            .build(object : CacheLoader<Any, List<String>>() {
                @Throws(IOException::class)
                override fun load(s: Any): List<String> {
                    val result = getInstitusjoner()
                    return result.sortedWith(compareBy({it},{it }))
                }
            })

    //reload cache
    fun refreshAll() {
        try {
            instituitioncache.refresh("")
            logger.debug("instituitioncache refresh")
            buccache.refresh("")
            logger.debug("buccache refresh")
        } catch (ex: Exception) {
            logger.error("Something went wrong cache")
        }
    }

    fun getBuC(euSaksnr: String): String? {
//        val urlpath = "/BuC"
//        val data = "?RINASaksnummer=$euSaksnr"
//        return oidcRestTemplate.getForEntity<String>("$EUX_PATH$urlpath$data", typeRef<String>()).body
        throw NotImplementedError("getBuC not yet implemented")
    }

    fun getCachedBuCTypePerSekor(): List<String> {
        logger.debug("hotcount: ${buccache.stats().hitCount()} totalLoadtime: ${buccache.stats().totalLoadTime()}")
        return buccache.get("")

    }

    fun getBuCtypePerSektor(): List<String> {
        val urlPath = "/BuCTypePerSektor"

        val headers = logonBasis()
        val httpEntity = HttpEntity("", headers)
        val response = oidcRestTemplate.exchange("$EUX_PATH$urlPath", HttpMethod.GET, httpEntity, typeRef<List<String>>())

        return response.body!!
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

    //henter ut status på rina.
    fun getRinaSaker(bucType: String = "",rinaNummer: String? = ""): List<RINASaker> {
        val urlPath = "/RINASaker"

        val builder = UriComponentsBuilder.fromPath("$EUX_PATH$urlPath")
                .queryParam("BuCType", bucType)
                .queryParam("RINASaksnummer", rinaNummer)

        val headers = logonBasis()

        val httpEntity = HttpEntity("", headers)
        val response = oidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, String::class.java)
        val responseBody = response.body!!
        try {
            if (response.statusCode.isError) {
                throw createErrorMessage(responseBody)
            } else {
                return objectMapper.readValue(responseBody, typeRefs<List<RINASaker>>())
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex.message)
        }
    }

    //Henter en liste over tilgjengelige aksjoner for den aktuelle RINA saken PK-51365"
    fun getMuligeAksjoner(euSaksnr: String): String {
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
                return responseBody // objectMapper.readValue(responseBody, typeRefs<String>())
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex.message)
        }
    }

    //Cached list of Institusjoner..
    fun getCachedInstitusjoner(): List<String> {
        val result = instituitioncache.get("")
        return result
    }

    //PK-51002 --
    //Henter ut en liste over registrerte institusjoner innenfor spesifiserte EU-land. PK-51002"
    fun getInstitusjoner(bucType: String = "", landKode: String = ""): List<String> {
        val urlPath = "/Institusjoner"

        val builder = UriComponentsBuilder.fromPath("$EUX_PATH$urlPath")
                .queryParam("BuCType", bucType)
                .queryParam("LandKode", landKode)

        val headers = logonBasis()
        val httpEntity = HttpEntity("", headers)

        val response = oidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, typeRef<String>())
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

    fun getAvailableSEDonBuc(buc: String?): List<String> {
        val list1 = listOf("P2000","P2200")
        val list2 = listOf("P6000","P10000")
        val list3 = listOf("P5000")

        val map : Map<String, List<String>> =
                mapOf(
                        "P_BUC_01" to list1,
                        "P_BUC_06" to list2,
                        "P_BUC_07" to list3
                )
        if (buc.isNullOrEmpty()) {
            val set: MutableSet<String> = Sets.newHashSet()
            set.addAll(list1)
            set.addAll(list2)
            set.addAll(list3)
            return Lists.newArrayList(set)
        }

        val result = map.get(buc).orEmpty()
        return result
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

        val headers = logonBasis()
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val httpEntity = HttpEntity(map, headers)
        val response = oidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
        return response.body
    }

    var overrideheaders: Boolean? = null

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
