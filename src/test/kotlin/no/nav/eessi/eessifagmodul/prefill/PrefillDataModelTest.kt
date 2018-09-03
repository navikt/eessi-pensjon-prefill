package no.nav.eessi.eessifagmodul.prefill

import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.PersonIkkeFunnetException
import no.nav.eessi.eessifagmodul.services.AktoerregisterException
import no.nav.eessi.eessifagmodul.services.AktoerregisterService
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class PrefillDataModelTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillDataModelTest::class.java) }

    @Mock
    private lateinit var mockAktoerregisterService: AktoerregisterService

    private lateinit var prefill: PrefillDataModel

    @Before
    fun setup() {
        logger.debug("Starting tests..")
        prefill = PrefillDataModel(mockAktoerregisterService)
    }

    @Test
    fun `check for none valid etterlatt`() {
        //val prefill = PrefillDataModel()
        assertFalse(prefill.isValidEtterlatt())


    }
    @Test
    fun `check for none valid etterlatt mangler pin`() {
        //val prefill = PrefillDataModel()
        prefill.setEtterlattAktoerID("123123")
        assertFalse(prefill.isValidEtterlatt())
    }
    @Test
    fun `check for none valid etterlatt mangler aktoerid`() {
        //val prefill = PrefillDataModel()
        prefill.setEtterlattPinID("23123")
        assertFalse(prefill.isValidEtterlatt())
    }
    @Test
    fun `check for none valid etterlatt begge er blank`() {
        prefill.setEtterlattPinID("")
        prefill.setEtterlattAktoerID("")
        assertFalse(prefill.isValidEtterlatt())
    }

    @Test
    fun `check for valid etterlatt`() {
        //val prefill = PrefillDataModel()
        prefill.setEtterlattAktoerID("123123")
        prefill.setEtterlattPinID("23123")
        assertTrue(prefill.isValidEtterlatt())
    }

    @Test
    fun `validate and check model build`() {
        val res = "9"
        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId("567890")).thenReturn(res)

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefill.build(
                subject = "Pensjon",
                sedID = "P6000",
                caseId = "12345",
                buc = "P_BUC_06",
                aktoerID = "567890",
                institutions = items
        )
        assertNotNull(prefill)
        assertEquals("P6000", prefill.getSEDid())
        assertEquals("P6000", prefill.getSED().sed)
        assertEquals("12345", prefill.getSaksnr())
        assertEquals("567890", prefill.getAktoerid())
        assertEquals("9", prefill.getPinid())

    }

    @Test(expected = AktoerregisterException::class)
    fun `create and test notvalid pinid for aktoerid`() {
        val exp = AktoerregisterException("Ident ikke funnet")
        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId("-5")).thenThrow(exp)

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefill.build(
                subject = "Pensjon",
                sedID = "P2000",
                caseId = "12345",
                buc = "P_BUC_06",
                aktoerID = "-5",
                institutions = items
        )
        //preutfylling.prefill(utfyllingMock)

    }

    @Test
    fun `create and test valid pinid for aktoerid`() {
        val res = "39"

        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(ArgumentMatchers.anyString())).thenReturn(res)
        //prefill.aktoerIdClient = mockAktoerIdClient

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefill.build(
                subject = "Pensjon",
                sedID = "P6000",
                caseId = "12345",
                buc = "P_BUC_06",
                aktoerID = "32",
                institutions = items
        )
        assertNotNull(prefill)
        assertNotNull(prefill.getPinid())
        assertEquals("39", prefill.getPinid())
    }




}