package no.nav.eessi.pensjon.prefill.models

import no.nav.eessi.pensjon.prefill.models.KrrPerson.Companion.validateEmail
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KrrPersonTest {

    val epostSomInneholderFeil = listOf("test_1@example.com", "somethin@domain", "somethin@domain.c", "somethin_@domain.com")
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
            val epost = email.validateEmail("4.3")
            println("Validerer: $email mot $epost")
            assertEquals(email, epost)
        }
    }

    @Test
    fun `validateEmail skal gi null ved ugyldig epost`() {
        epostSomInneholderFeil.forEach { email ->
            versjoner.forEach{ versjon ->
                println("Validerer: $email for versjon: $versjon")
                assertNull(email.validateEmail(versjon))
            }
        }
    }
}
