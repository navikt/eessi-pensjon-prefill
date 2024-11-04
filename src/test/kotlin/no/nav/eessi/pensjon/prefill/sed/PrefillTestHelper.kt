package no.nav.eessi.pensjon.prefill.sed


import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.BucType
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjonRequestBuilder
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjonsinformasjonClient
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.InstitusjonItem
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

object PrefillTestHelper {

    fun lesPensjonsdataVedtakFraFil(responseXMLfilename: String): PensjonsinformasjonService {
        val pensjonsinformasjonRestTemplate = mockk<RestTemplate>()
        every {
            pensjonsinformasjonRestTemplate.exchange(
                any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)
            )
        } returns readXMLVedtakresponse(responseXMLfilename)

        val pensjonsinformasjonClient = PensjonsinformasjonClient(pensjonsinformasjonRestTemplate, PensjonRequestBuilder())
        return PensjonsinformasjonService(pensjonsinformasjonClient)
    }

    fun lesPensjonsdataFraFil(responseXMLfilename: String): PensjonsinformasjonService {
        val pensjonsinformasjonRestTemplate = mockk<RestTemplate>()
        every {
            pensjonsinformasjonRestTemplate.exchange(
                any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)
            )
        } returns readXMLresponse(responseXMLfilename)

        val pensjonsinformasjonClient = PensjonsinformasjonClient(pensjonsinformasjonRestTemplate, PensjonRequestBuilder())
        return PensjonsinformasjonService(pensjonsinformasjonClient)
    }

    fun readJsonResponse(file: String): String {
        return javaClass.getResource(file).readText()
    }

    fun readXMLresponse(file: String): ResponseEntity<String> {
        val resource = javaClass.getResource(file).readText()
        return ResponseEntity(resource, HttpStatus.OK)
    }

    fun readXMLVedtakresponse(file: String): ResponseEntity<String> {
        val resource = javaClass.getResource(file).readText()
//        val resource = ResourceUtils.getFile("classpath:pensjonsinformasjon/vedtak/$file").readText()
        return ResponseEntity(resource, HttpStatus.OK)
    }


    fun createMockApiRequest(sed: SedType, buc: BucType, payload: String, sakNr: String): ApiRequest {
        val items = listOf(InstitusjonItem(country = "NO", institution = "NAVT003"))
        return ApiRequest(
                institutions = items,
                sed = sed,
                sakId = sakNr,
                euxCaseId = null,
                aktoerId = "1000060964183",
                buc = buc,
                subjectArea = "Pensjon",
                payload = payload,
                processDefinitionVersion = "4.2"
        )
    }

}
