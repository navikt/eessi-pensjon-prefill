package no.nav.eessi.pensjon.fagmodul.models

import no.nav.eessi.pensjon.utils.eessiRequire
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertFalse

class SEDTypeTest {
    @Test
    fun `check for value on SEDtype`() {
        val px = "P3000_SE"
        val result = SEDType.isValidSEDType(px)
        assertTrue(result)
    }

    @Test
    fun `check for uvalid SEDtype to prefill`() {
        val px = "P3000"
        val result = SEDType.isValidSEDType(px)
        assertFalse(result)

        assertThrows<ResponseStatusException> {
            eessiRequire(SEDType.isValidSEDType(px)) { "Kaste en tull melding under test"}
        }

    }

}
