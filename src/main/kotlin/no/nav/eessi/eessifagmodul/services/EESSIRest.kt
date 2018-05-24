package no.nav.eessi.eessifagmodul.services

import org.apache.cxf.endpoint.Client
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.web.client.RestTemplate
import org.springframework.http.client.support.BasicAuthorizationInterceptor
import org.springframework.web.util.UriTemplate
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory


class EESSIRest {

    val logger: Logger by lazy { LoggerFactory.getLogger(EESSIRest::class.java)}

    val timeout : Int = 100000

    lateinit var build : RestTemplateBuilder
    lateinit var url : String
    lateinit var authorization : BasicAuthorizationInterceptor
    lateinit var restTemplate : RestTemplate

    fun getRest() : RestTemplate {
        return restTemplate // build.rootUri(url).build()
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
        return authorization
    }

    inline fun <reified T: Any> typeRef(): ParameterizedTypeReference<T> = object: ParameterizedTypeReference<T>(){}

    fun createGet(path : String) : RequestEntity<Any> {
        return genericRequest(path, HttpMethod.GET)
    }

    fun createPost(path: String) : RequestEntity<Any> {
        return genericRequest(path, HttpMethod.POST)
    }

    fun genericRequest(path: String, method : HttpMethod) : RequestEntity<Any> {
        //val uri = URI(url + path)
        val uri = UriTemplate(url).expand(path)
        val req : RequestEntity<Any> = RequestEntity(method,  uri)
        return req
    }

//    fun <T: Any> get(path : String, tref : Any) : ResponseEntity<Any> {
//        return getRest().exchange(createGet(path), typeRef<tref>())
//    }
//    fun <T : RequestEntity<T>> prefixer(data : OpprettBuCogSEDRequest) : (OpprettBuCogSEDRequest, RequestEntity<T>) -> T {
//    }

     fun getTimedRest(rest: RestTemplate) : RestTemplate?  {
         val simpleFactory : ClientHttpRequestFactory = rest.requestFactory

         if (simpleFactory is SimpleClientHttpRequestFactory) {
             simpleFactory.setReadTimeout(timeout)
             simpleFactory.setConnectTimeout(timeout)
         }

        return rest
    }

    fun getSimpleFactory(fac : ClientHttpRequestFactory) :SimpleClientHttpRequestFactory? {
        if (fac is SimpleClientHttpRequestFactory) {
            return fac
        }
        return null
    }

//    override fun toString(): String {
//        println("URL :  $url")
//        println("restTemp : " + getRest())
//        return "EESSIRest : $url  $build " + build.rootUri(url).build()
//    }

}

