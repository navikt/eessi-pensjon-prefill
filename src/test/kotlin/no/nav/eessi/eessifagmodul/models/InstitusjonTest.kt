package no.nav.eessi.eessifagmodul.models

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(JUnit4::class)
class InstitusjonTest {

    @Test
    fun validerEnkelInstitusjon() {
        val data = Institusjon("SE", "Sverige")

        assertNotNull(data)
        assertEquals("SE", data.landkode)
        assertEquals("Sverige", data.navn)
    }

    @Test
    fun validerEnkelInstitusjonTomNavn() {
        val data = Institusjon("DK", "")

        assertNotNull(data)
        assertEquals("DK", data.landkode)
        assertEquals("", data.navn)
    }
    @Test
    fun validerEnkelInstitusjonTom() {
        val data = Institusjon("", null)

        assertNotNull(data)
        assertNotNull("", data.landkode)
        assertNull(data.navn)
    }

    @Test
    fun validerEnkelInstitusjonNull() {
        val data = Institusjon(null, null)

        assertNotNull(data)
        assertNull(data.landkode)
        assertNull(data.navn)
    }


}