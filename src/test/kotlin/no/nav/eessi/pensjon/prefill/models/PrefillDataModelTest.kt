package no.nav.eessi.pensjon.prefill.models

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillDataModelTest {

    private lateinit var prefillDatamodel: PrefillDataModel

    @BeforeEach
    fun setup() {
        prefillDatamodel = PrefillDataModelMother.initialPrefillDataModel(
            penSaksnummer = "123123",
            pinId = "1231231",
            sedType = SedType.SEDTYPE_P2000,
            vedtakId = "323232",
            euxCaseId = "231233213123",
            avdod = PersonInfo("12312312", "23123")
        )
    }

    @Test
    fun `check for valid claimant fail`() {
        assertNotNull(prefillDatamodel.avdod)
    }

    @Test
    fun `check for valid claimant pin and aktorid is blank fail`() {
        assertEquals(prefillDatamodel.avdod?.norskIdent, "12312312")
    }

    @Test
    fun `validate and check model build`() {
        assertNotNull(prefillDatamodel)
        assertEquals(SedType.SEDTYPE_P2000, prefillDatamodel.sedType)
        assertEquals("123123", prefillDatamodel.penSaksnummer)
        assertEquals("123456789", prefillDatamodel.bruker.aktorId)
        assertEquals("1231231", prefillDatamodel.bruker.norskIdent)
    }

}
