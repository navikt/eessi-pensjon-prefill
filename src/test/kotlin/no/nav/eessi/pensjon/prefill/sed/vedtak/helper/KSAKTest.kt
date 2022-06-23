package no.nav.eessi.pensjon.prefill.sed.vedtak.helper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KSAKTest {

    @Test
    fun `sjekke enum correct value`() {
        assertEquals(KSAK.ALDER, KSAK.valueOf("ALDER"))
    }
}
