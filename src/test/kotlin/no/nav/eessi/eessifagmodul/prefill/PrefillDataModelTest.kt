package no.nav.eessi.eessifagmodul.prefill

import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.clients.aktoerid.AktoerIdClient
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.PersonIkkeFunnetException
import no.nav.eessi.eessifagmodul.models.SED
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

    private lateinit var prefill: PrefillDataModel

    @Before
    fun setup() {
        logger.debug("Starting tests..")
        prefill = PrefillDataModel()
    }

    @Test
    fun `check for none valid etterlatt`() {
        assertFalse(prefill.isValidEtterlatt())
    }

    @Test
    fun `check for none valid etterlatt mangler pin`() {
        //val prefill = PrefillDataModel()
        prefill.avdodAktoerID = "123123"
        assertFalse(prefill.isValidEtterlatt())
    }
    @Test
    fun `check for none valid etterlatt mangler aktoerid`() {
        //val prefill = PrefillDataModel()
        prefill.avdodPersonnr = "23123"
        assertFalse(prefill.isValidEtterlatt())
    }
    @Test
    fun `check for none valid etterlatt begge er blank`() {
        prefill.avdodAktoerID = ""
        prefill.avdodPersonnr = ""
        assertFalse(prefill.isValidEtterlatt())
    }

    @Test
    fun `check for valid etterlatt`() {
        //val prefill = PrefillDataModel()
        prefill.avdodAktoerID = "123123"
        prefill.avdodPersonnr = "23123"
        assertTrue(prefill.isValidEtterlatt())
    }

    @Test
    fun `validate and check model build`() {
        val res = "9"

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefill.build(
                subject = "Pensjon",
                sedID = "P6000",
                caseId = "12345",
                buc = "P_BUC_06",
                aktoerID = "567890",
                pinID = "123456789",
                institutions = items
        )
        assertNotNull(prefill)
        assertEquals("P6000", prefill.getSEDid())
        assertEquals(SED::class.java , prefill.sed.javaClass)
        assertEquals("12345", prefill.penSaksnummer)
        assertEquals("567890", prefill.aktoerID)
        assertEquals("123456789", prefill.personNr)



    }


    @Test
    fun `create and test valid pinid for aktoerid`() {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefill.build(
                subject = "Pensjon",
                sedID = "P6000",
                caseId = "12345",
                buc = "P_BUC_06",
                aktoerID = "32",
                pinID = "1234000001",
                institutions = items
        )
        assertNotNull(prefill)
        assertNotNull(prefill.personNr)
        assertEquals("1234000001", prefill.personNr)
    }




}