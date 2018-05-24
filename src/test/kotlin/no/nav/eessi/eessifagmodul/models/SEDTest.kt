package no.nav.eessi.eessifagmodul.models

import org.junit.Test
import kotlin.test.assertEquals

class SEDTest {

    @Test
    fun normalSED() {
        val samboer = NavPerson("345")
        val sed = SED(
                SEDType = "P6000",
                NAVSaksnummer = "12345678",
                ForsikretPerson = NavPerson("1234567891011"),
                Barn = listOf(NavPerson("123"), NavPerson("234")),
                Samboer = samboer
        )


    assertEquals("P6000", sed.SEDType)
    assertEquals(NavPerson::class.java, sed.ForsikretPerson::class.java)
    assertEquals(NavPerson::class.java, sed.Samboer!!::class.java)
    assertEquals("1234567891011", sed.ForsikretPerson.fnr)
    assertEquals(samboer, sed.Samboer)
        val test : NavPerson = sed.Samboer!!
        assertEquals(samboer.fnr, test.fnr)
    }

    @Test
    fun otherSED() {
        val person : NavPerson = NavPerson("12345")

        val sed = SED(
            SEDType ="TYPE",
            NAVSaksnummer = "SAKSNR",
            ForsikretPerson = person,
            Barn = null,
            Samboer = null)


        assertEquals("TYPE", sed.SEDType)
        assertEquals("SAKSNR", sed.NAVSaksnummer)
        assertEquals(person.fnr, sed.ForsikretPerson.fnr)
        assertEquals(null, sed.Samboer)
        assertEquals(null, sed.Barn)
    }




}