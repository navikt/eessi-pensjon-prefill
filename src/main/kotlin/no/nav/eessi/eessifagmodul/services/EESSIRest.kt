package no.nav.eessi.eessifagmodul.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.web.client.RestTemplate
import org.springframework.http.client.support.BasicAuthorizationInterceptor
import java.net.URI

class EESSIRest {

    val logger: Logger by lazy { LoggerFactory.getLogger(EESSIRest::class.java)}

    lateinit var build : RestTemplateBuilder
    lateinit var url : String
    lateinit var auth : BasicAuthorizationInterceptor
    lateinit var resttmp : RestTemplate

    fun getRest() : RestTemplate {
        return resttmp // build.rootUri(url).build()
    }
    fun getRest(timeout : Int) : RestTemplate {
        return build.rootUri(url).setConnectTimeout(timeout).setReadTimeout(timeout).build()
    }

    fun getURL() : String {
        return url
    }

    fun getRestBuilder() : RestTemplateBuilder {
        return build
    }

    fun getAuthx() : BasicAuthorizationInterceptor {
        return auth
    }

//    fun <T: Any> get(path : String, tref : Any) : ResponseEntity<Any> {
//        return getRest().exchange(createGet(path), typeRef<tref>())
//    }

    inline fun <reified T: Any> typeRef(): ParameterizedTypeReference<T> = object: ParameterizedTypeReference<T>(){}

    fun createGet(path : String) : RequestEntity<Any> {
        val uri = URI(url + path)
        val req : RequestEntity<Any> = RequestEntity(HttpMethod.GET,  uri)
        return req
    }

//    override fun toString(): String {
//        println("URL :  $url")
//        println("restTemp : " + getRest())
//        return "EESSIRest : $url  $build " + build.rootUri(url).build()
//    }

}

