package no.nav.eessi.eessifagmodul.preutfyll

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.clients.aktoerid.AktoerIdClient
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.eessi.eessifagmodul.services.EuxServiceTest
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class PreutfyllingTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PreutfyllingTest::class.java) }

    @Mock
    private lateinit var mockAktoerIdClient: AktoerIdClient

    @Mock
    private lateinit var mockPreutfyllingNav: PreutfyllingNav

    @Mock
    private lateinit var mockPreutfyllingPensjon: PreutfyllingPensjon

    @Mock
    private lateinit var sed: SED

    lateinit var preutfylling: Preutfylling

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        preutfylling = Preutfylling(aktoerIdClient = mockAktoerIdClient, preutfyllingNav = mockPreutfyllingNav, preutfyllingPensjon = mockPreutfyllingPensjon)
    }


    @Test
    fun `create mock on preutfyll P6000`() {
        val request = FrontendRequest(
                sed = "P6000",
                caseId = "12345",
                pinid = "1234",
                subjectArea = "Pensjon"

        )

        val response = HentIdentForAktoerIdResponse()
        response.ident = "1234"
        whenever(mockAktoerIdClient.hentIdentForAktoerId(ArgumentMatchers.anyString())).thenReturn(response)

        val navresponse = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy")))
        whenever(mockPreutfyllingNav.utfyllNav(any())).thenReturn(navresponse)

        val pensjonresponse = Pensjon(gjenlevende = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy")))
        whenever(mockPreutfyllingPensjon.pensjon(any())).thenReturn(pensjonresponse)

        val utfyllingData = preutfylling.preutfylling(request)

        assertNotNull(utfyllingData)

        assertNotNull(utfyllingData.sed)

        assertNotNull(utfyllingData.sed.nav)
        assertNotNull(utfyllingData.sed.nav?.bruker)
        assertNotNull(utfyllingData.sed.nav?.bruker?.person)

        assertEquals("Dummy", utfyllingData.sed.nav?.bruker?.person?.etternavn)
        assertEquals("Dummy", utfyllingData.sed.nav?.bruker?.person?.fornavn)

        println(mapAnyToJson(utfyllingData.sed, true))

    }








}