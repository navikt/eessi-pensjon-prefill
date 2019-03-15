package no.nav.eessi.eessifagmodul.services.pensjonsinformasjon

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.utils.simpleFormat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.ResourceUtils
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ActiveProfiles("test")
@RunWith(MockitoJUnitRunner.Silent::class)
class PensjonsinformasjonServiceTest {

    @Mock
    private lateinit var mockrestTemplate: RestTemplate

    lateinit var pensjonsinformasjonService: PensjonsinformasjonService

    @Before
    fun setup() {
        pensjonsinformasjonService = PensjonsinformasjonService(mockrestTemplate, RequestBuilder())
        pensjonsinformasjonService.fasitenv = "q1"
    }

    @Test
    fun hentAlt() {
        val mockResponseEntity = createResponseEntityFromJsonFile("classpath:pensjonsinformasjon/full-generated-response.xml")
        whenever(mockrestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))).thenReturn(mockResponseEntity)
        val data = pensjonsinformasjonService.hentAltPaaVedtak("1243")
        // TODO: add asserts

        assertNotNull(data.vedtak, "Vedtak er null")
        assertEquals("2016-09-11", data.vedtak.virkningstidspunkt.simpleFormat())
    }

    @Test
    fun hentAltFeilersaaProverViIgjen() {
        val mockResponseEntity = createResponseEntityFromJsonFile("classpath:pensjonsinformasjon/full-generated-response.xml")

        whenever(mockrestTemplate.exchange(
                any<String>(),
                any(),
                any<HttpEntity<Unit>>(),
                ArgumentMatchers.eq(String::class.java))
        ).thenThrow(HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "dummy error"))
                .thenReturn(mockResponseEntity)

        val data = pensjonsinformasjonService.hentAltPaaVedtak("1243")
        // TODO: add asserts

        assertNotNull(data.vedtak, "Vedtak er null")
        assertEquals("2016-09-11", data.vedtak.virkningstidspunkt.simpleFormat())
    }


    private fun createResponseEntityFromJsonFile(filePath: String, httpStatus: HttpStatus = HttpStatus.OK): ResponseEntity<String?> {
        val mockResponseString = ResourceUtils.getFile(filePath).readText()
        return ResponseEntity(mockResponseString, httpStatus)
    }

    @Test
    fun `Sjekker om pensjoninformasjon XmlCalendar kan være satt eller null også sette simpleFormat`() {
        val mockResponseEntity = createResponseEntityFromJsonFile("classpath:pensjonsinformasjon/full-generated-response.xml")
        whenever(mockrestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))).thenReturn(mockResponseEntity)
        val data = pensjonsinformasjonService.hentAltPaaVedtak("1243")

        var result = data.ytelsePerMaanedListe.ytelsePerMaanedListe.get(0)

        assertEquals("2008-02-06", result.fom.simpleFormat())
        assertEquals("2015-08-04", result.tom?.let { it.simpleFormat() })

        result = data.ytelsePerMaanedListe.ytelsePerMaanedListe.get(1)

        assertNotNull(result)
        assertNotNull(result.fom)

        assertEquals("2008-02-06", result.fom.simpleFormat())
        assertEquals(null, result.tom?.let { it.simpleFormat() })

    }

}