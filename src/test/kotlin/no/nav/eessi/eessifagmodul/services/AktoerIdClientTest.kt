package no.nav.eessi.eessifagmodul.services

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentRequest
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner::class)
@ActiveProfiles("test")
class AktoerIdClientTest {

    @InjectMocks
    lateinit var aktorclient : AktoerIdClient

    @Mock
    lateinit var service: AktoerV2

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun hentAktoerIdForIdent() {
        val identid = "ident12"

        val request = HentAktoerIdForIdentRequest()
        request.ident = identid

        val response = HentAktoerIdForIdentResponse()
        response.aktoerId = "Aktor12345"

        whenever(service.hentAktoerIdForIdent(any())).thenReturn(response)
        aktorclient.service = service

        assertEquals(service, aktorclient.service)

        val res = aktorclient.hentAktoerIdForIdent(identid)!!
        assertNotNull(res)
        assertEquals(response.aktoerId, res.aktoerId)

    }

}

