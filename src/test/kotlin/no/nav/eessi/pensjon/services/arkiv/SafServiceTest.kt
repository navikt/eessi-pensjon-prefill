package no.nav.eessi.pensjon.services.arkiv

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.nio.file.Paths

@ExtendWith(MockitoExtension::class)
class SafServiceTest {

    @Mock
    private lateinit var safGraphQlOidcRestTemplate: RestTemplate

    @Mock
    private lateinit var safRestOidcRestTemplate: RestTemplate

    lateinit var safService: SafService

    @BeforeEach
    fun setup() {
        safService = SafService(safGraphQlOidcRestTemplate, safRestOidcRestTemplate)
    }

    @Test
    fun `gitt en gyldig hentMetadata reponse når metadata hentes så map til HentMetadataResponse`() {
        val responseJson = String(Files.readAllBytes(Paths.get("src/test/resources/json/saf/hentMetadataResponse.json")))

        whenever(safGraphQlOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java)))
                .thenReturn(ResponseEntity(responseJson, HttpStatus.OK))
        val resp = safService.hentDokumentMetadata("1234567891000")

        val mapper = jacksonObjectMapper()
        JSONAssert.assertEquals(mapper.writeValueAsString(resp), responseJson, true)
    }

    @Test
    fun `gitt noe annet enn 200 httpCopde feil når metadata hentes så kast SafException med tilhørende httpCode`() {
        whenever(safGraphQlOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java)))
                .thenReturn(ResponseEntity("", HttpStatus.NOT_FOUND))
        assertThrows<SafException> {
            safService.hentDokumentMetadata("1234567891000")
        }
    }

    @Test
    fun `gitt en feil når metadata hentes så kast SafException med tilhørende httpCode`() {
        whenever(safGraphQlOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java)))
                .thenThrow(RestClientException("some error"))
        assertThrows<SafException> {
            safService.hentDokumentMetadata("1234567891000")
        }
    }

    @Test
    fun `gitt noe annet enn 200 httpCopde feil når dokumentinnhold hentes så kast SafException med tilhørende httpCode`() {
        whenever(safRestOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java)))
                .thenReturn(ResponseEntity("", HttpStatus.NOT_FOUND))
        assertThrows<SafException> {
            safService.hentDokumentInnhold("123", "456", VariantFormat.ARKIV)
        }
    }

    @Test
    fun `gitt en feil når dokumentinnhold hentes så kast SafException med tilhørende httpCode`() {
        whenever(safRestOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java)))
                .thenThrow(RestClientException("some error"))
        assertThrows<SafException> {
            safService.hentDokumentInnhold("123", "456", VariantFormat.ARKIV)
        }
    }
}
