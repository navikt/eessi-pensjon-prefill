package no.nav.eessi.pensjon.prefill

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter

@ExtendWith(OutputCaptureExtension::class)
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

    @Test
    fun `feil eller tom JSON gir avvik`(output: CapturedOutput) {
        for (json in listOf("""{"sed":"P8000","sedVer":""", "", "   ")) {
            assertFalse(kontroll.kontrollerSedVersjon(json))
        }
        assertTrue(output.all.contains("Klarte ikke å lese SED: feilaktig eller tom JSON"))
    }

    @Test
    fun `JSON i feil format gir avvik`() {
        val body = """{"sed":"P8000","sedVer":"""

        assertSame(
            body,
            kontroll.beforeBodyWrite(
                body,
                mockk(),
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter::class.java,
                mockk(),
                mockk()
            )
        )
    }
}
