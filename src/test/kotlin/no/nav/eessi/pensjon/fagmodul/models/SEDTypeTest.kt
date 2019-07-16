package no.nav.eessi.pensjon.fagmodul.models

import org.junit.Test
import kotlin.test.assertTrue

class SEDTypeTest {
    @Test
    fun `check for value on SEDtype`() {
        val px = "P3000"
        val result = SEDType.isValidSEDType(px)
        assertTrue(result)
    }
}