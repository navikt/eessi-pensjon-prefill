package no.nav.eessi.eessifagmodul.services

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SedMock
import no.nav.eessi.eessifagmodul.models.createSED
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.prefill.PrefillNav
import no.nav.eessi.eessifagmodul.prefill.PrefillPerson
import no.nav.eessi.eessifagmodul.prefill.PrefillSED
import no.nav.eessi.eessifagmodul.services.eux.EuxService
import no.nav.eessi.eessifagmodul.services.eux.RinaActions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
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

    lateinit var mockPrefillSED: PrefillSED

    lateinit var mockPrefillDataModel: PrefillDataModel

    lateinit var prefillService: PrefillService


    @Before
    fun `startup initilize testing`() {
        mockPrefillDataModel = PrefillDataModel()

        mockPrefillSED = PrefillSED(mockPrefillPerson)

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