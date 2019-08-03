package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import org.junit.Test
import kotlin.test.assertEquals

class KSAKTest {

    @Test
    fun `sjekke enum correct value`() {
        assertEquals(KSAK.ALDER, KSAK.valueOf("ALDER"))
    }
}
