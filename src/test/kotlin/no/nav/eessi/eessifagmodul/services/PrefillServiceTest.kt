package no.nav.eessi.eessifagmodul.services

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.*
import no.nav.eessi.eessifagmodul.prefill.P6000.PrefillP6000
import no.nav.eessi.eessifagmodul.services.eux.EuxService
import no.nav.eessi.eessifagmodul.services.eux.RinaActions
import no.nav.eessi.eessifagmodul.utils.SedEnum
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.http.HttpStatus
import java.lang.IllegalArgumentException
import java.net.UnknownHostException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PrefillServiceTest {


    @Mock
    lateinit var mockRinaActions: RinaActions

    @Mock
    lateinit var mockEuxService: EuxService

    @Mock
    lateinit var mockPrefillSED: PrefillSED

    private lateinit var prefillService: PrefillService

    @Before
    fun `startup initilize testing`() {
        prefillService = PrefillService(mockEuxService, mockPrefillSED, mockRinaActions)

    }

    @Test
    fun `mock prefillAndAddSedOnExistingCase valid`() {
        val dataModel = generatePrefillModel()
        dataModel.euxCaseID = "1234567890"

        val resultData = generatePrefillModel()
        resultData.sed = generateMockP2000(dataModel)

        whenever(mockPrefillSED.prefill(any())).thenReturn(resultData)
        whenever(mockRinaActions.canCreate(any() ,any() )).thenReturn(true)
        whenever(mockRinaActions.canUpdate(any() ,any() )).thenReturn(true)
        whenever(mockEuxService.createSEDonExistingRinaCase(any(), any(), any())).thenReturn(HttpStatus.OK)

        val result = prefillService.prefillAndAddSedOnExistingCase(dataModel)

        assertNotNull(result)
        assertEquals(dataModel.euxCaseID, result.euxCaseID)

    }

    @Test(expected = UnknownHostException::class)
    fun `mock prefillAndAddSedOnExistingCase euxserver exception`() {
        val dataModel = generatePrefillModel()
        dataModel.euxCaseID = "1234567890"

        val resultData = generatePrefillModel()
        resultData.sed = generateMockP2000(dataModel)

        whenever(mockPrefillSED.prefill(any())).thenReturn(resultData)
        whenever(mockRinaActions.canCreate(any() ,any() )).thenReturn(true)
        whenever(mockEuxService.createSEDonExistingRinaCase(any(), any(), any())).thenThrow(UnknownHostException::class.java)
        prefillService.prefillAndAddSedOnExistingCase(dataModel)
    }

    @Test(expected = SedDokumentIkkeGyldigException::class)
    fun `mock prefillAndAddSedOnExistingCase checkCanCreate fail`() {
        val dataModel = generatePrefillModel()
        dataModel.euxCaseID = "1234567890"

        val resultData = generatePrefillModel()
        resultData.sed = generateMockP2000(dataModel)

        whenever(mockPrefillSED.prefill(any())).thenReturn(resultData)
        whenever(mockRinaActions.canCreate(any() ,any() )).thenReturn(false)

        prefillService.prefillAndAddSedOnExistingCase(dataModel)

    }

    @Test(expected = SedDokumentIkkeOpprettetException::class)
    fun `mock prefillAndAddSedOnExistingCase checkCanUpdate fail`() {
        val dataModel = generatePrefillModel()
        dataModel.euxCaseID = "1234567890"

        val resultData = generatePrefillModel()
        resultData.sed = generateMockP2000(dataModel)

        whenever(mockPrefillSED.prefill(any())).thenReturn(resultData)
        whenever(mockRinaActions.canCreate(any() ,any() )).thenReturn(true)
        whenever(mockRinaActions.canUpdate(any() ,any() )).thenReturn(false)
        whenever(mockEuxService.createSEDonExistingRinaCase(any(), any(), any())).thenReturn(HttpStatus.OK)

        prefillService.prefillAndAddSedOnExistingCase(dataModel)

    }

    @Test
    fun `mock prefillAndCreateSedOnNewCase valid`() {
        val dataModel = generatePrefillModel()
        dataModel.euxCaseID = "1234567890"

        val resultData = generatePrefillModel()
        resultData.sed = generateMockP2000(dataModel)

        whenever(mockPrefillSED.prefill(any())).thenReturn(resultData)
        whenever(mockRinaActions.canUpdate(any() ,any() )).thenReturn(true)
        whenever(mockEuxService.createCaseAndDocument(any(), any(), any(),any(),any(),any())).thenReturn(dataModel.euxCaseID)

        val result = prefillService.prefillAndCreateSedOnNewCase(resultData)
        assertEquals("{\"euxcaseid\":\"1234567890\"}", result.euxCaseID)

    }

    @Test(expected = SedDokumentIkkeOpprettetException::class)
    fun `mock prefillAndCreateSedOnNewCase checkForUpdateStatus fail`() {
        val dataModel = generatePrefillModel()
        dataModel.euxCaseID = "1234567890"

        val resultData = generatePrefillModel()
        resultData.sed = generateMockP2000(dataModel)

        whenever(mockPrefillSED.prefill(any())).thenReturn(resultData)
        whenever(mockRinaActions.canUpdate(any() ,any() )).thenReturn(false)
        whenever(mockEuxService.createCaseAndDocument(any(), any(), any(),any(),any(),any())).thenReturn(dataModel.euxCaseID)

        prefillService.prefillAndCreateSedOnNewCase(resultData)

    }


    @Test
    fun `mock prefillSed valid value`() {
        val mockPrefillDataModel = generatePrefillModel()

        val returnData = generatePrefillModel()
        returnData.sed = generateMockP2000(mockPrefillDataModel)

        whenever(mockPrefillSED.prefill(any())).thenReturn(returnData)

        val result = prefillService.prefillSed(mockPrefillDataModel)

        assertNotNull(result)
        assertEquals("P2000", result.getSEDid())
        assertEquals("Gul", result.sed.nav?.bruker?.person?.fornavn)
        assertEquals("Konsoll", result.sed.nav?.bruker?.person?.etternavn)
    }

    fun generateMockP2000(prefillModel: PrefillDataModel): SED {
        val mocksed = prefillModel.sed
        val mockp2000 = SedMock().genererP2000Mock()
        mocksed.nav = mockp2000.nav
        mocksed.pensjon = mockp2000.pensjon
        return mocksed
    }

    fun generatePrefillModel(): PrefillDataModel {
        return PrefillDataModel().apply {
            euxCaseID = "1000"
            sed = SED().create("P2000")
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

    }

}