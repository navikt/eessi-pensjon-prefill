package no.nav.eessi.pensjon.fagmodul.prefill.model

import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PrefillDataModelTest {

    private lateinit var prefill: PrefillDataModel

    @BeforeEach
    fun setup() {
        prefill = PrefillDataModel(penSaksnummer = "12345")
    }

    @Test
    fun `check for valid claimant fail`() {
        assertFalse(prefill.erGyldigEtterlatt())
    }

    @Test
    fun `check for valid claimant pin and aktorid is blank fail`() {
        prefill.avdodNorskIdent = ""
        assertFalse(prefill.erGyldigEtterlatt())
    }

    @Test
    fun `check for valid claimant deceased pin and aktorid is filled`() {
        prefill.avdodNorskIdent = "23123"
        assertTrue(prefill.erGyldigEtterlatt())
    }

    @Test
    fun `check for valid claimant deceased parents filled`() {
        prefill.avdodFar = "2312354"
        prefill.avdodMor = "2312376"
        assertTrue(prefill.erForeldreLos())
    }

    @Test
    fun `validate and check model build`() {

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefill.apply {
                rinaSubject = "Pensjon"
                sed =  SED("vedtak")
                buc = "P_BUC_06"
                aktorId = "567890"
                norskIdent = "123456789"
                institution = items
        }
        assertNotNull(prefill)
        assertEquals("vedtak", prefill.getSEDid())
        assertEquals(SED::class, prefill.sed::class)
        assertEquals("12345", prefill.penSaksnummer)
        assertEquals("567890", prefill.aktorId)
        assertEquals("123456789", prefill.norskIdent)
    }


    @Test
    fun `create and test valid pinid for aktoerid`() {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefill.apply {
            rinaSubject= "Pensjon"
            sed = SED("vedtak")
            buc = "P_BUC_06"
            aktorId = "32"
            norskIdent = "1234000001"
            institution = items
        }
        assertNotNull(prefill)
        assertNotNull(prefill.norskIdent)
        assertEquals("1234000001", prefill.norskIdent)
    }

}
