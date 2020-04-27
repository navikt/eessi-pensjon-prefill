package no.nav.eessi.pensjon.fagmodul.prefill.model

import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PrefillDataModelTest {

    private lateinit var prefillDatamodel: PrefillDataModel

    @BeforeEach
    fun setup() {
        prefillDatamodel = PrefillDataModel(penSaksnummer = "12345", bruker = PersonId("123456789", "567890"), avdod = PersonId("123","4556"))
    }

    @Test
    fun `check for valid claimant fail`() {
        assertNotNull(prefillDatamodel.avdod)
    }

    @Test
    fun `check for valid claimant pin and aktorid is blank fail`() {
        assertEquals(prefillDatamodel.avdod?.norskIdent, "123")
    }

    @Test
    fun `validate and check model build`() {

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefillDatamodel.apply {
                rinaSubject = "Pensjon"
                sed =  SED("vedtak")
                buc = "P_BUC_06"
                institution = items
        }
        assertNotNull(prefillDatamodel)
        assertEquals("vedtak", prefillDatamodel.getSEDid())
        assertEquals(SED::class, prefillDatamodel.sed::class)
        assertEquals("12345", prefillDatamodel.penSaksnummer)
        assertEquals("567890", prefillDatamodel.bruker.aktorId)
        assertEquals("123456789", prefillDatamodel.bruker.norskIdent)
    }

}
