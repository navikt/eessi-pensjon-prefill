package no.nav.eessi.eessifagmodul.models

import org.junit.Test
import kotlin.test.assertEquals

class SEDTest {

    @Test
    fun normalSED() {
        val sed = SED(
                SEDType = "P6000",
                NAVSaksnummer = "12345678",
                ForsikretPerson = NavPerson("1234567891011"),
                Barn = listOf(NavPerson("123"), NavPerson("234")),
                Samboer = NavPerson("345")
        )


    assertEquals("P6000", sed.SEDType)
    assertEquals(NavPerson::class.java, sed.ForsikretPerson::class.java)
    assertEquals(NavPerson::class.java, sed.Samboer!!::class.java)
    assertEquals("1234567891011", sed.ForsikretPerson.fnr)
    }

}