package no.nav.eessi.eessifagmodul.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class NavFodselsnummerTest {

    @Test
    fun `valid check for age`() {
        val fnr = generateRandomFnr(48)
        println("RandomFnr: $fnr")
        val actualDate = LocalDate.now().minusYears(48)

        val navfnr = NavFodselsnummer(fnr)
        assertEquals(48, navfnr.getAge())
        assertEquals(false, navfnr.getValidPentionAge())

        assertEquals(actualDate, navfnr.getBirthDate())

        val dtf = DateTimeFormatter.ofPattern("YYYY-MM-dd")
        val exprectedFormat = actualDate.format(dtf)
        val actualFormat = navfnr.getBirthDate().format(dtf)
        assertEquals(exprectedFormat, actualFormat)
    }

    @Test
    fun `valid check for old age`() {
        val fnr = generateRandomFnr(72)
        println("RandomFnr: $fnr")

        val navfnr = NavFodselsnummer(fnr)
        assertEquals(72, navfnr.getAge())
        assertEquals(true, navfnr.getValidPentionAge())
    }


    @Test
    fun `valid pention age`() {
        val fnr = generateRandomFnr(67)
        println("RandomFnr: $fnr")
        val navfnr = NavFodselsnummer(fnr)

        assertEquals(67, navfnr.getAge())
        assertEquals(true, navfnr.getValidPentionAge())
    }

    @Test
    fun `not valid pention age`() {
        val fnr = generateRandomFnr(66)
        println("RandomFnr: $fnr")

        val navfnr = NavFodselsnummer(fnr)
        assertEquals(false, navfnr.getValidPentionAge())
    }

    @Test
    fun `valid pention very old age`() {
        val fnr = generateRandomFnr(20, "533")
        println("RandomFnr: $fnr")
        val navfnr = NavFodselsnummer(fnr)

        assertEquals("1898", navfnr.get4DigitBirthYear())
        assertEquals(120, navfnr.getAge())
        assertEquals(true, navfnr.getValidPentionAge())
    }

    @Test
    fun `not valid pention very young age`() {
        val fnr = generateRandomFnr(10, "501")
        println("RandomFnr: $fnr")
        val navfnr = NavFodselsnummer(fnr)

        assertEquals("2008", navfnr.get4DigitBirthYear())
        assertEquals(10, navfnr.getAge())
        assertEquals(false, navfnr.getValidPentionAge())
    }

    @Test
    fun `not valid pention age young age`() {
        val fnr = generateRandomFnr(25)
        println("RandomFnr: $fnr")

        val navfnr = NavFodselsnummer(fnr)
        assertEquals(false, navfnr.getValidPentionAge())
    }

    @Test
    fun `not valid pention age young age with DNR`() {
        val fnr = generateRandomFnr(25)
        val newfnr = mockDnr(fnr)

        val navfnr = NavFodselsnummer(newfnr)
        assertEquals(25, navfnr.getAge())
        assertEquals(false, navfnr.getValidPentionAge())
    }

    @Test
    fun `not valid pention age young age2`() {
        val fnr = generateRandomFnr(25)
        println("RandomFnr: $fnr")
        val navfnr = NavFodselsnummer(fnr)

        assertEquals(25, navfnr.getAge())
        assertEquals(false, navfnr.getValidPentionAge())
    }


    private fun generateRandomFnr(yearsToSubtract: Int, indivdnr: String = "496"): String {
        val fnrdate = LocalDate.now().minusYears(yearsToSubtract.toLong())
        val y = fnrdate.year.toString()
        val day = fixDigits(fnrdate.dayOfMonth.toString())
        val month = fixDigits(fnrdate.month.value.toString())
        val fixedyear = y.substring(2, y.length)
        println(day + month + fixedyear + indivdnr + "52")
        return day + month + fixedyear + indivdnr + "52" //43352
    }

    private fun mockDnr(strFnr: String): String {
        val nvf = NavFodselsnummer(strFnr)
        val fdig = nvf.getFirstDigit(strFnr)
        return when (fdig) {
            0 -> "4" + strFnr.substring(1, strFnr.length)
            1 -> "5" + strFnr.substring(1, strFnr.length)
            2 -> "6" + strFnr.substring(1, strFnr.length)
            3 -> "7" + strFnr.substring(1, strFnr.length)
            else -> strFnr
        }
    }

    private fun fixDigits(str: String): String {
        if (str.length == 1) {
            return "0$str"
        }
        return str
    }

}