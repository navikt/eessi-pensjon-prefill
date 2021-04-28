package no.nav.eessi.pensjon.fagmodul.prefill.sed


import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.prefill.ApiRequest
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjonsinformasjonClient
import no.nav.eessi.pensjon.services.pensjonsinformasjon.RequestBuilder

import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate

object PrefillTestHelper {

    fun lesPensjonsdataVedtakFraFil(responseXMLfilename: String): PensjonsinformasjonService {
        val pensjonsinformasjonRestTemplate = mockk<RestTemplate>()
        every {
            pensjonsinformasjonRestTemplate.exchange(
                any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)
            )
        } returns readXMLVedtakresponse(responseXMLfilename)

        val pensjonsinformasjonClient = PensjonsinformasjonClient(pensjonsinformasjonRestTemplate, RequestBuilder())
        pensjonsinformasjonClient.initMetrics()
        return PensjonsinformasjonService(pensjonsinformasjonClient)
    }

    fun lesPensjonsdataFraFil(responseXMLfilename: String): PensjonsinformasjonService {
        val pensjonsinformasjonRestTemplate = mockk<RestTemplate>()
        every {
            pensjonsinformasjonRestTemplate.exchange(
                any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)
            )
        } returns readXMLresponse(responseXMLfilename)

        val pensjonsinformasjonClient = PensjonsinformasjonClient(pensjonsinformasjonRestTemplate, RequestBuilder())
        pensjonsinformasjonClient.initMetrics()
        return PensjonsinformasjonService(pensjonsinformasjonClient)
    }

    fun readJsonResponse(file: String): String {
        return ResourceUtils.getFile("classpath:json/nav/$file").readText()
    }

    fun readXMLresponse(file: String): ResponseEntity<String> {
        val resource = ResourceUtils.getFile("classpath:pensjonsinformasjon/krav/$file").readText()
        return ResponseEntity(resource, HttpStatus.OK)
    }

    fun readXMLVedtakresponse(file: String): ResponseEntity<String> {
        val resource = ResourceUtils.getFile("classpath:pensjonsinformasjon/vedtak/$file").readText()
        return ResponseEntity(resource, HttpStatus.OK)
    }


    fun createMockApiRequest(sedName: String, buc: String, payload: String, sakNr: String): ApiRequest {
        val items = listOf(InstitusjonItem(country = "NO", institution = "NAVT003"))
        return ApiRequest(
                institutions = items,
                sed = sedName,
                sakId = sakNr,
                euxCaseId = null,
                aktoerId = "1000060964183",
                buc = buc,
                subjectArea = "Pensjon",
                payload = payload
        )
    }

}
