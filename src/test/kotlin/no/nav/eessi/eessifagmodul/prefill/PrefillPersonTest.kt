package no.nav.eessi.eessifagmodul.prefill

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.aktoer.v2.feil.PersonIkkeFunnet
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(Parameterized::class)
class PrefillPersonTest(val index: Int, val sedid: String) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPersonTest::class.java) }

    @Mock
    private lateinit var mockPreutfyllingNav: PrefillNav

    @Mock
    private lateinit var mockPreutfyllingPensjon: PrefillPensjon

    @Mock
    private lateinit var sed: SED

    lateinit var preutfylling: PrefillPerson

    lateinit var prefillDataMock: PrefillDataModel

    @Before
    fun setup() {
        prefillDataMock = PrefillDataModel()
        logger.debug("Starting tests.... ...")
        MockitoAnnotations.initMocks(this)
        preutfylling = PrefillPerson(prefillNav = mockPreutfyllingNav, prefilliPensjon = mockPreutfyllingPensjon)
    }

    companion object {
        @Parameters
        @JvmStatic
        fun data(): Collection<Array<Any>> {
            return listOf(
                    arrayOf(1, "P2000"),
                    arrayOf(2, "P4000"),
                    arrayOf(3, "P5000"),
                    arrayOf(4, "P6000")
            )
        }
    }

    @Test
    fun `create mock on preutfyll SED`() {
        logger.debug("jobber med test på følgende sed: $sedid")
        val mockPinResponse = "12345"

        //whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(ArgumentMatchers.anyString())).thenReturn(response)

        val navresponse = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy", pin = listOf(PinItem(sektor = "alle", identifikator = mockPinResponse, land = "NO")))))
        whenever(mockPreutfyllingNav.utfyllNav(any())).thenReturn(navresponse)

        val pensjonresponse = Pensjon(gjenlevende = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy")))
        whenever(mockPreutfyllingPensjon.pensjon(any())).thenReturn(pensjonresponse)

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefillDataMock.build(
                subject = "Pensjon",
                sedID = sedid,
                caseId = "12345",
                buc = "P_BUC_06",
                aktoerID = "1234",
                pinID = "12345",
                institutions = items
        )

        val responseSED = preutfylling.prefill(prefillDataMock)
        println(mapAnyToJson(responseSED, true))

        assertNotNull(responseSED)
        assertNotNull(responseSED.nav)
        assertNotNull(responseSED.nav?.bruker)
        assertNotNull(responseSED.nav?.bruker?.person)

        assertEquals("Dummy", responseSED.nav?.bruker?.person?.etternavn)
        assertEquals("Dummy", responseSED.nav?.bruker?.person?.fornavn)
        val pin = responseSED.nav?.bruker?.person?.pin
        assertEquals(mockPinResponse, pin!![0].identifikator)
        assertEquals(sedid, responseSED.sed)
        assertNotNull(prefillDataMock)
        assertEquals(mockPinResponse, prefillDataMock.personNr)

    }



}