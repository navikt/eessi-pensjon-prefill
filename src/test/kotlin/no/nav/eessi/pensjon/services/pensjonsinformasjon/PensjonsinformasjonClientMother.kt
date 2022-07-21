package no.nav.eessi.pensjon.services.pensjonsinformasjon

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjonRequestBuilder
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjonsinformasjonClient
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate

object PensjonsinformasjonClientMother {

    fun fraFil(responseXMLfilename: String): PensjonsinformasjonClient {
        val resource = ResourceUtils.getFile("classpath:pensjonsinformasjon/vedtak/$responseXMLfilename").readText()
        val readXMLresponse = ResponseEntity(resource, HttpStatus.OK)

        val mockRestTemplate: RestTemplate = mockk()
/*
        whenever(mockRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java)))
                .thenReturn(readXMLresponse)
*/
        every { mockRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns readXMLresponse
        val pensjonsinformasjonClient = PensjonsinformasjonClient(mockRestTemplate, PensjonRequestBuilder())
        pensjonsinformasjonClient.initMetrics()
        return pensjonsinformasjonClient
    }

}
