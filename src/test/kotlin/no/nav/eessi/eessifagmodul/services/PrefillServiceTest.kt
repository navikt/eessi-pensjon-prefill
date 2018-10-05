package no.nav.eessi.eessifagmodul.services

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SedMock
import no.nav.eessi.eessifagmodul.models.createSED
import no.nav.eessi.eessifagmodul.prefill.*
import no.nav.eessi.eessifagmodul.services.eux.EuxService
import no.nav.eessi.eessifagmodul.services.eux.RinaActions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PrefillServiceTest {


    @Mock
    lateinit var mockRinaActions: RinaActions

    @Mock
    lateinit var mockEuxService: EuxService

    @Mock
    lateinit var mockPrefillPerson: PrefillPerson

    @Mock
    lateinit var pre2000: PrefillP2000
    @Mock
    lateinit var pre4000: PrefillP4000
    @Mock
    lateinit var pre6000: PrefillP6000
    @Mock
    lateinit var preDefault: PrefillDefaultSED

    private lateinit var mockPrefillSED: PrefillSED
    private lateinit var mockPrefillDataModel: PrefillDataModel
    private lateinit var prefillService: PrefillService
    private lateinit var prefillFactory: PrefillFactory

    @Before
    fun `startup initilize testing`() {
        mockPrefillDataModel = PrefillDataModel()

        pre2000 = PrefillP2000(mockPrefillPerson)

        prefillFactory = PrefillFactory()
        prefillFactory .prefill2000 = pre2000
        prefillFactory .prefill4000 = pre4000
        prefillFactory .prefill6000 = pre6000
        prefillFactory .prefillDefault = preDefault

        mockPrefillSED = PrefillSED(prefillFactory)
        prefillService = PrefillService(mockEuxService, mockPrefillSED, mockRinaActions)

    }


    @Test
    fun `test prefillSed valid value`() {

        mockPrefillDataModel.apply {
            euxCaseID = "1000"
            sed = createSED("P2000")
            buc  = "P_BUC_01"
            institution = listOf(
                    InstitusjonItem(
                            country = "NO",
                            institution = "DUMMY"
                    )
            )
            penSaksnummer = "123456789999"
            personNr = "12345678901"
        }

        val mocksed = mockPrefillDataModel.sed
        val mockp2000 = SedMock().genererP2000Mock()
        mocksed.nav = mockp2000.nav
        mocksed.pensjon = mockp2000.pensjon

        whenever(mockPrefillPerson.prefill(any() )).thenReturn(mocksed)
        val result = prefillService.prefillSed(mockPrefillDataModel)

        assertNotNull(result)
        assertEquals(mockPrefillDataModel.sed, mocksed)
        assertEquals("P2000", result.getSEDid())
        assertEquals("Gul", result.sed.nav?.bruker?.person?.fornavn)
        assertEquals("Konsoll", result.sed.nav?.bruker?.person?.etternavn)
    }


}