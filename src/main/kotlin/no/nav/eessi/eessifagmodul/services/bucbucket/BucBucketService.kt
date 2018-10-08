package no.nav.eessi.eessifagmodul.services.bucbucket

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.eessi.eessifagmodul.models.SED
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

// TODO: Work-in-progress. Venter på at grensesnittet dokumenteres og implementeres i eux-bucbucket

@Service
class BucBucketService(val bucBucketOidcRestTemplate: RestTemplate) {

    fun queryDocuments(queryParams: Map<QueryParameters, String>): List<QueryResult> {
        val responseEntity = doRequest("/seds", queryParams)

        val queryResult = jacksonObjectMapper().readValue<List<QueryResult>>(responseEntity.body!!)
        return queryResult
    }

    fun getDocument(correlationId: String): SED {
        val response = doRequest("/documents", mapOf(QueryParameters.CORRELATION_ID to correlationId))
        return jacksonObjectMapper().readValue(response.body!!)
    }

    private fun doRequest(path: String, queryParams: Map<QueryParameters, String>): ResponseEntity<String> {
        val uriBuilder = UriComponentsBuilder.fromPath(path)
        queryParams.forEach {
            uriBuilder.queryParam(it.key.paramName(), it.value)
        }
        val responseEntity = bucBucketOidcRestTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET,null, String::class.java)
        return responseEntity
    }
}
