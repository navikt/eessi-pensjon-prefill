package no.nav.eessi.pensjon.security.sts

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItemTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

@ExtendWith(MockitoExtension::class)
class STSServiceTest {

    @Mock
    private lateinit var securityTokenExchangeBasicAuthRestTemplate: RestTemplate

    private lateinit var stsService: STSService

    @BeforeEach
    fun oppStart() {

        stsService = STSService(securityTokenExchangeBasicAuthRestTemplate)

    }


    @Test
    fun tester() {


    }

    @Test
    fun getSystemOidcToken_withValidToken() {

        val mockSecurityTokenResponse = SecurityTokenResponse(
                accessToken = "LKUITDKUo96tyfhj",
                tokenType = "sts",
                expiresIn = 10L
        )


        val response = ResponseEntity.ok().body(mockSecurityTokenResponse)

        whenever(securityTokenExchangeBasicAuthRestTemplate.exchange(
                ArgumentMatchers.anyString(),
                eq(HttpMethod.GET),
                eq(null),
                eq(typeRef<SecurityTokenResponse>()))
        ).thenReturn(response)

        val result = stsService.getSystemOidcToken()

        assertEquals(mockSecurityTokenResponse.accessToken, result)

    }

    @Test
    fun getSystemOidcToken_withError() {

        val mockSecurityTokenResponse = SecurityTokenResponse(
                accessToken = "LKUITDKUo96tyfhj",
                tokenType = "sts",
                expiresIn = 10L
        )

        val response = ResponseEntity.badRequest().body(mockSecurityTokenResponse)

        whenever(securityTokenExchangeBasicAuthRestTemplate.exchange(
                ArgumentMatchers.anyString(),
                eq(HttpMethod.GET),
                eq(null),
                eq(typeRef<SecurityTokenResponse>()))
        ).thenReturn(response)

        assertThrows<SystembrukerTokenException> {
            stsService.getSystemOidcToken()
        }
    }

}
