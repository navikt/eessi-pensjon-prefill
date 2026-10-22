package no.nav.eessi.pensjon.prefill

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SedVersjonKontrollTest {

    private val kontroll = SedVersjonKontroll("4")

    @Test
    fun `SED med forventet sedVer gir ingen avvik`() {
        assertTrue(kontroll.kontrollerSedVersjon("""{"sed":"P8000","sedGVer":"4","sedVer":"4"}"""))
    }

    @Test
    fun `SED med annen eller manglende sedVer gir avvik`() {
        for (sedVer in listOf("\"2\"", "\"5\"", "null")) {
            assertFalse(kontroll.kontrollerSedVersjon("""{"sed":"P8000","sedGVer":"4","sedVer":$sedVer}"""), "sedVer=$sedVer")
        }
        assertFalse(kontroll.kontrollerSedVersjon("""{"sed":"P8000"}"""))
    }
}
