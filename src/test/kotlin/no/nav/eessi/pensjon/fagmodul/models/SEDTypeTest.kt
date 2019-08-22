package no.nav.eessi.pensjon.fagmodul.models

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SEDTypeTest {
    @Test
    fun `check for value on SEDtype`() {
        val px = "P3000"
        val result = SEDType.isValidSEDType(px)
        assertTrue(result)
    }
}
