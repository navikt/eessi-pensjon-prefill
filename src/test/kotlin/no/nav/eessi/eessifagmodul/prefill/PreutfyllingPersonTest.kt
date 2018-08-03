package no.nav.eessi.eessifagmodul.prefill

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.clients.aktoerid.AktoerIdClient
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.aktoer.v2.feil.PersonIkkeFunnet
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
class PreutfyllingPersonTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PreutfyllingPersonTest::class.java) }

    @Mock
    private lateinit var mockAktoerIdClient: AktoerIdClient

    @Mock
    private lateinit var mockPreutfyllingNav: PrefillNav

    @Mock
    private lateinit var mockPreutfyllingPensjon: PrefillPensjon

    @Mock
    private lateinit var sed: SED

    lateinit var preutfylling: PrefillPerson

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        preutfylling = PrefillPerson(aktoerIdClient = mockAktoerIdClient, prefillNav = mockPreutfyllingNav, prefilliPensjon = mockPreutfyllingPensjon)
    }


    @Test
    fun `create mock on preutfyll P6000`() {
        val response = HentIdentForAktoerIdResponse()
        response.ident = "1234"

        whenever(mockAktoerIdClient.hentIdentForAktoerId(ArgumentMatchers.anyString())).thenReturn(response)

        val navresponse = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy", pin = listOf(PinItem(sektor = "alle", identifikator = response.ident, land = "NO")))))
        whenever(mockPreutfyllingNav.utfyllNav(any())).thenReturn(navresponse)

        val pensjonresponse = Pensjon(gjenlevende = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy")))
        whenever(mockPreutfyllingPensjon.pensjon(any())).thenReturn(pensjonresponse)

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        val utfyllingMock = PrefillDataModel().build(
                subject = "Pensjon",
                sedID = "P6000",
                caseId = "12345",
                buc = "P_BUC_06",
                aktoerID = "1234",
                institutions = items
        )
        val responseSED = preutfylling.preutfyll(utfyllingMock)
        println(mapAnyToJson(responseSED, true))

        assertNotNull(responseSED)

        assertNotNull(responseSED.nav)
        assertNotNull(responseSED.nav?.bruker)
        assertNotNull(responseSED.nav?.bruker?.person)

        assertEquals("Dummy", responseSED.nav?.bruker?.person?.etternavn)
        assertEquals("Dummy", responseSED.nav?.bruker?.person?.fornavn)
        val pin = responseSED.nav?.bruker?.person?.pin
        assertEquals("1234", pin!![0].identifikator)

    }

    @Test(expected = PersonIkkeFunnetException::class)
    fun `create and test valid pinid for aktoerid`() {
        val faultInfo = PersonIkkeFunnet()
        val exp = HentIdentForAktoerIdPersonIkkeFunnet("Ident For AktoerId Ikke funnet", faultInfo)
        whenever(mockAktoerIdClient.hentIdentForAktoerId("-2")).thenThrow(exp)

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        val utfyllingMock = PrefillDataModel().build(
                subject = "Pensjon",
                sedID = "P6000",
                caseId = "12345",
                buc = "P_BUC_06",
                aktoerID = "-2",
                institutions = items
        )
        preutfylling.preutfyll(utfyllingMock)
    }


}