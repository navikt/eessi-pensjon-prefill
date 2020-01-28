package no.nav.eessi.pensjon.services.kodeverk

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.pensjon.utils.toJson
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder
import java.util.*

@Service
class KodeverkService(private val kodeRestTemplate: RestTemplate,
                      @Value("\${app.name}") private val appName: String) {

    private val logger = LoggerFactory.getLogger(KodeverkService::class.java)

    fun hentKodeverk(kodeverknavn: String) : String {
        val path = "/api/v1/kodeverk/{kodeverksnavn}/koder"
        val uriParams = mapOf("kodeverksnavn" to kodeverknavn)
        val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)

        return doRequest(builder)
    }

    fun hentHierarki(hierarki: String) : String {
        //https://kodeverk.nais.adeo.no/api/v1/hierarki/LandkoderSammensattISO2/noder
        val path = "/api/v1/hierarki/{hierarki}/noder"

        val uriParams = mapOf("hierarki" to hierarki)
        val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)

        //LandkoderSammensattISO2
        //curl -X GET "https://kodeverk.nais.adeo.no/api/v1/hierarki/LandkoderSammensattISO2/noder" -H "accept: application/json;charset=UTF-8" -H "Nav-Call-Id: eessi-pen" -H "Nav-Consumer-Id: eessi-pen"

        logger.debug("henter Hierakri: $builder")

        return doRequest(builder)
    }


    fun doRequest(builder: UriComponents) : String {
        return try {
            val headers = HttpHeaders()
            headers["Nav-Consumer-Id"] = appName
            headers["Nav-Call-Id"] = UUID.randomUUID().toString()
            val requestEntity = HttpEntity<String>(headers)
            logger.debug("Header: $requestEntity")
            val response = kodeRestTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    requestEntity,
                    String::class.java)

            response.body?.let { return it } ?: {
                throw KodeverkException("Feil ved konvetering av jsondata fra kodeverk")
            }()

        } catch (ce: HttpClientErrorException) {
            throw KodeverkException(ce.message!!)
        } catch (se: HttpServerErrorException) {
            throw KodeverkException(se.message!!)
        } catch (ex: Exception) {
            throw KodeverkException(ex.message!!)
        }

    }

    private fun hentLandKode(): List<Landkode> {
        val tmpLandkoder = hentHierarki("LandkoderSammensattISO2")

        val rootNode = jacksonObjectMapper().readTree(tmpLandkoder)
        val noder = rootNode.at("/noder").toList()
        return  noder.map { node -> Landkode(node.at("/kode").textValue(),
                node.at("/undernoder").findPath("kode").textValue()) }.sortedBy { (sorting, _) -> sorting }.toList()
    }

    fun hentAlleLandkoder() = hentLandKode().toJson()  // test

    fun hentLandkoderAlpha2() = hentLandKode().map { it.landkode2 }.toList() //test

    fun finnLandkode2(alpha3: String): String? {
        val list = hentLandKode()
        list.forEach {
            if (it.landkode3 == alpha3) {
                return it.landkode2
            }
        }
        return null
    }

    fun finnLandkode3(alpha2: String): String? {
        val list = hentLandKode()
        list.forEach {
            if (it.landkode2 == alpha2) {
                return it.landkode3
            }
        }
        return null

    }

}

data class Landkode (
        val landkode2: String, // SE
        val landkode3: String // SWE
)

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class KodeverkIkkeFunnetException(message: String) : RuntimeException(message)

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
class KodeverkException(message: String) : RuntimeException(message)
