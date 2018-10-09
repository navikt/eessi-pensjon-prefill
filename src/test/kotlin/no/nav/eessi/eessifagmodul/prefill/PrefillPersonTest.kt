package no.nav.eessi.eessifagmodul.prefill

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
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
    lateinit var preutfylling: PrefillPerson

    private lateinit var mockPrefillSED: PrefillSED

    private lateinit var prefill2000: PrefillP2000
    private lateinit var prefill2100: PrefillP2100
    private lateinit var prefill2200: PrefillP2200
    private lateinit var prefillDefault: PrefillDefaultSED

    private lateinit var prefillDataMock: PrefillDataModel
    private lateinit var mockPrefillFactory: PrefillFactory

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        logger.debug("Starting tests.... ...")

        prefillDataMock = PrefillDataModel()
        preutfylling = PrefillPerson(prefillNav = mockPreutfyllingNav, prefilliPensjon = mockPreutfyllingPensjon)

        prefillDefault = PrefillDefaultSED(preutfylling)
        prefill2000 = PrefillP2000(preutfylling)
        prefill2100 = PrefillP2100(preutfylling)
        prefill2200 = PrefillP2200(preutfylling)

        mockPrefillFactory = PrefillFactory()
        mockPrefillFactory.prefill2000 = prefill2000
        mockPrefillFactory.prefill2100 = prefill2100
        mockPrefillFactory.prefill2200 = prefill2200
        mockPrefillFactory.prefillDefault = prefillDefault

        mockPrefillSED = PrefillSED(mockPrefillFactory)

    }

    companion object {
        @Parameters
        @JvmStatic
        fun `collection data`(): Collection<Array<Any>> {
            return listOf(
                    arrayOf(1, "P2000"),
                    arrayOf(2, "P2100"),
                    arrayOf(3, "P2200"),
                    //arrayOf(4, "P4000"),
                    arrayOf(5, "P5000"),
                    arrayOf(6, "P6000"),
                    arrayOf(7, "P7000")
            )
        }
    }

    @Test
    fun `create mock on prefill SED`() {
        logger.debug("\n\njobber med test på følgende sed: $sedid\n\n")
        val mockPinResponse = "12345"

        val navresponse = NavMock().genererNavMock()
        //val navresponse = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy", pin = listOf(PinItem(sektor = "alle", identifikator = mockPinResponse, land = "NO")))))

        whenever(mockPreutfyllingNav.prefill(any())).thenReturn(navresponse)

        val pensjonresponse = PensjonMock().genererMockData()
        //val pensjonresponse = Pensjon(gjenlevende = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy")))
        whenever(mockPreutfyllingPensjon.prefill(any())).thenReturn(pensjonresponse)

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefillDataMock.apply {
                rinaSubject = "Pensjon"
                sed = createSED(sedid)
                penSaksnummer = "12345"
                buc = "P_BUC_06"
                aktoerID = "1234"
                personNr = "12345"
                institution = items
        }

        val responseData = mockPrefillSED.prefill(prefillDataMock)
        assertNotNull(responseData)

        //val responseSED = preutfylling.prefill(prefillDataMock)
        val responseSED = responseData.sed

        //println(mapAnyToJson(responseSED, true))

        assertNotNull(responseSED)
        assertNotNull(responseSED.nav)
        assertNotNull(responseSED.nav?.bruker)
        assertNotNull(responseSED.nav?.bruker?.person)

        assertEquals("Konsoll", responseSED.nav?.bruker?.person?.etternavn)
        assertEquals("Gul", responseSED.nav?.bruker?.person?.fornavn)
        assertEquals("1967-12-01", responseSED.nav?.bruker?.person?.foedselsdato)
        assertEquals("asfsdf", responseSED.nav?.bruker?.mor?.person?.fornavn)

        val pin = responseSED.nav?.bruker?.person?.pin
        assertEquals("weqrwerwqe", pin!![0].identifikator)
        assertEquals("sdfsdfsdfsdf sdfsdfsdf", responseSED.nav?.bruker?.bank?.navn)

        assertEquals(sedid, responseSED.sed)
        assertNotNull(prefillDataMock)
        assertEquals(mockPinResponse, prefillDataMock.personNr)

    }

}