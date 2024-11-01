package no.nav.eessi.pensjon.prefill.models

import no.nav.eessi.pensjon.prefill.models.KrrPerson.Companion.validateEmail
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KrrPersonTest {

    val epostSomInneholderFeil = listOf("somethin-email", "somethin@domain", "somethin@domain.c", "somethin_g@domain.com")
    val versjoner = listOf("4.2", "4.1")

    @Test
    fun `validateEmail skal kun gi epost tilbake ved gyldig epost`() {
        val validEmail = "test.email@example.com"
        versjoner.forEach{ versjon ->
            assertEquals(validEmail, validEmail.validateEmail(versjon))
        }
    }

    @Test
    fun `validateEmail skal gi epost for 43 med ugyldige tegn`() {
        epostSomInneholderFeil.forEach { email ->
            assertEquals(email, email.validateEmail("4.3"))
        }
    }

    @Test
    fun `validateEmail skal gi null ved ugyldig epost`() {
        epostSomInneholderFeil.forEach { email ->
            versjoner.forEach{ versjon ->
                assertNull(email.validateEmail(versjon))
            }
        }
    }
}
