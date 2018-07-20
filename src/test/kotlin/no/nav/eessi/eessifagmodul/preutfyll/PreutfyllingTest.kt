package no.nav.eessi.eessifagmodul.preutfyll

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.clients.aktoerid.AktoerIdClient
import no.nav.eessi.eessifagmodul.models.*
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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

    lateinit var preutfylling: PreutfyllingPerson

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        preutfylling = PreutfyllingPerson(aktoerIdClient = mockAktoerIdClient, preutfyllingNav = mockPreutfyllingNav, preutfyllingPensjon = mockPreutfyllingPensjon)
    }


    @Test
    fun `create mock on preutfyll P6000`() {
//        val request = FrontendRequest(
//                sed = "P6000",
//                caseId = "12345",
//                pinid = "1234",
//                subjectArea = "Pensjon"
//
//        )

        val response = HentIdentForAktoerIdResponse()
        response.ident = "1234"

        whenever(mockAktoerIdClient.hentIdentForAktoerId(ArgumentMatchers.anyString())).thenReturn(response)

        val navresponse = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy")))
        whenever(mockPreutfyllingNav.utfyllNav(any())).thenReturn(navresponse)

        val pensjonresponse = Pensjon(gjenlevende = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy")))
        whenever(mockPreutfyllingPensjon.pensjon(any())).thenReturn(pensjonresponse)

        val utfyllingMock = UtfyllingData().mapFromRequest(
                subject = "Pensjon",
                sedID = "P6000",
                caseId = "12345",
                buc = "P_BUC_06",
                aktoerID = "1234"
        )
        val responseSED = preutfylling.preutfyll(utfyllingMock)

        assertNotNull(responseSED)

        assertNotNull(responseSED.nav)
        assertNotNull(responseSED.nav?.bruker)
        assertNotNull(responseSED.nav?.bruker?.person)

        assertEquals("Dummy", responseSED.nav?.bruker?.person?.etternavn)
        assertEquals("Dummy", responseSED.nav?.bruker?.person?.fornavn)

        println(mapAnyToJson(responseSED, true))

    }








}