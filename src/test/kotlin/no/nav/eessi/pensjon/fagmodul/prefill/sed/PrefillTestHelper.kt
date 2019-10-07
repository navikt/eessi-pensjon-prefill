package no.nav.eessi.pensjon.fagmodul.prefill.sed

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.tps.BrukerFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.pensjon.services.pensjonsinformasjon.RequestBuilder
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.lenient
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate

object PrefillTestHelper {

    fun lesPensjonsdataFraFil(responseXMLfilename: String): PensjonsinformasjonHjelper {
        val pensjonsinformasjonRestTemplate = mock<RestTemplate>()
        lenient().`when`(pensjonsinformasjonRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))).thenReturn(readXMLresponse(responseXMLfilename))

        val pensjonsinformasjonService = PensjonsinformasjonService(pensjonsinformasjonRestTemplate, RequestBuilder())

        return PensjonsinformasjonHjelper(pensjonsinformasjonService)
    }

    val mockEessiInformasjon = EessiInformasjon(
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO",
            institutionGate = "Postboks 6600 Etterstad TEST",
            institutionBy = "Oslo",
            institutionPostnr = "0607",
            institutionLand = "NO"
    )

    fun setupPersondataFraTPS(mockPersonDataFraTPS: Set<PersonDataFromTPS.MockTPS>): BrukerFromTPS {
        val datatps = PersonDataFromTPS(mockPersonDataFraTPS, mockEessiInformasjon)
        return datatps.mockBrukerFromTPS()
    }

    fun readJsonResponse(file: String): String {
        return ResourceUtils.getFile("classpath:json/nav/$file").readText()
    }

    private fun readXMLresponse(file: String): ResponseEntity<String> {
        val resource = ResourceUtils.getFile("classpath:pensjonsinformasjon/krav/$file").readText()
        return ResponseEntity(resource, HttpStatus.OK)
    }
}
