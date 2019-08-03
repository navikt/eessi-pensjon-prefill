package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.pensjon.services.pensjonsinformasjon.RequestBuilder
import org.mockito.ArgumentMatchers
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate

object PrefillPensjonVedtakFromPENMother {

    fun fraFil(responseXMLfilename: String): PrefillPensjonVedtakFromPEN {
        val resource = ResourceUtils.getFile("classpath:pensjonsinformasjon/vedtak/$responseXMLfilename").readText()
        val readXMLresponse = ResponseEntity(resource, HttpStatus.OK)

        val mockRestTemplate = mock<RestTemplate>()
        whenever(mockRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java)))
                .thenReturn(readXMLresponse)

        return PrefillPensjonVedtakFromPEN(
                PensjonsinformasjonHjelper(
                        PensjonsinformasjonService(mockRestTemplate, RequestBuilder())))
    }

}
