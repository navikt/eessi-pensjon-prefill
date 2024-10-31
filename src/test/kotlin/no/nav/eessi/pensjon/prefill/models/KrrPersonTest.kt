package no.nav.eessi.pensjon.prefill.models

import no.nav.eessi.pensjon.prefill.models.KrrPerson.Companion.validateEmail
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KrrPersonTest {

    @Test
    fun `validateEmail skal kun gi epost tilbake ved gyldig epost`() {
        val validEmail = "test.email@example.com"
        assertEquals(validEmail, validEmail.validateEmail())
    }

    @Test
    fun `validateEmail skal gi null ved ugyldig epost`() {
        val invalidEmail1 = "somethin-email"
        val invalidEmail2 = "somethin@domain"
        val invalidEmail3 = "somethin@domain.c"
        val invalidEmail4 = "somethin_g@domain.com"
        assertNull(invalidEmail1.validateEmail())
        assertNull(invalidEmail2.validateEmail())
        assertNull(invalidEmail3.validateEmail())
        assertNull(invalidEmail4.validateEmail())
    }
}
