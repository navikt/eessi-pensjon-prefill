package no.nav.eessi.eessifagmodul.utils

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate


class NavFodselsnummerTest {


    @Test
    fun `valid check for age`() {
        val fnr = generateRandomFnr(48)
        val navfnr = NavFodselsnummer(fnr)

        assertEquals(48, navfnr.getAge())
        assertEquals(false, navfnr.getValidPentionAge())
    }

    @Test
    fun `valid check for old age`() {
        val fnr = generateRandomFnr(72)
        val navfnr = NavFodselsnummer(fnr)

        assertEquals(72, navfnr.getAge())
        assertEquals(true, navfnr.getValidPentionAge())
    }


    @Test
    fun `valid pention age`() {
        val fnr = generateRandomFnr(67)
        val navfnr = NavFodselsnummer(fnr)

        assertEquals(67, navfnr.getAge())
        assertEquals(true, navfnr.getValidPentionAge())
    }

    @Test
    fun `not valid pention age`() {
        val fnr = generateRandomFnr(66)
        val navfnr = NavFodselsnummer(fnr)

        assertEquals(false, navfnr.getValidPentionAge())
    }

    @Test
    fun `valid pention very old age`() {
        val fnr = generateRandomFnr(20, "533")
        val navfnr = NavFodselsnummer(fnr)

        assertEquals("1898", navfnr.get4DigitBirthYear())
        assertEquals(120, navfnr.getAge())
        assertEquals(true, navfnr.getValidPentionAge())
    }

    @Test
    fun `not valid pention very young age`() {
        val fnr = generateRandomFnr(10, "501")
        val navfnr = NavFodselsnummer(fnr)

        assertEquals("2008", navfnr.get4DigitBirthYear())
        assertEquals(10, navfnr.getAge())
        assertEquals(false, navfnr.getValidPentionAge())
    }

    @Test
    fun `not valid pention age young age`() {
        val fnr = generateRandomFnr(25)
        val navfnr = NavFodselsnummer(fnr)

        assertEquals(false, navfnr.getValidPentionAge())
    }

    @Test
    fun `not valid pention age young age with DNR`() {
        val fnr = generateRandomFnr(25)
        val newfnr = "" + "5" + fnr.substring(1,fnr.length)
        val navfnr = NavFodselsnummer(newfnr)

        assertEquals(24, navfnr.getAge())
        assertEquals(false, navfnr.getValidPentionAge())
    }

    private fun generateRandomFnr(yearsToSubtract: Int, indivdnr: String = "433" ) : String {
        val fnrdate = LocalDate.now().minusYears(yearsToSubtract.toLong())
        val y = fnrdate.year.toString()
        val day = fixDigits(fnrdate.dayOfMonth.toString())
        val month = fixDigits(fnrdate.month.value.toString())
        val fixedyear = y.substring(2,y.length)
        val fnr = day+month+fixedyear+indivdnr+ "52" //43352
        return fnr
    }

    private fun fixDigits(str: String): String {
        if (str.length == 1) {
            return "0$str"
        }
        return str
    }

}