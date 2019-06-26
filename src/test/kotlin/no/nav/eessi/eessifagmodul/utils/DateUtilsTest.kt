package no.nav.eessi.eessifagmodul.utils

import org.junit.Test
import java.time.format.DateTimeParseException
import javax.xml.datatype.DatatypeFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class DateUtilsTest {

    @Test
    fun `check XML calendar to Rina Date`() {
        val xmlcal = DatatypeFactory.newInstance().newXMLGregorianCalendar()
        xmlcal.month = 5
        xmlcal.year = 2020
        xmlcal.day = 20

        val toRinaDate = xmlcal.simpleFormat()

        assertNotNull(toRinaDate)
        assertEquals("2020-05-20", toRinaDate)
    }

    @Test
    fun `verify XML date is still 2016-01-01 to simpleFormat`() {
        val xmlcal = DatatypeFactory.newInstance().newXMLGregorianCalendar()
        xmlcal.month = 1
        xmlcal.year = 2016
        xmlcal.day = 1
        xmlcal.hour = 0
        xmlcal.minute = 0
        xmlcal.second = 1

        val toRinaDate = xmlcal.simpleFormat()

        assertNotNull(toRinaDate)
        assertEquals("2016-01-01", toRinaDate)
    }

    @Test
    fun `Test av konvertere datotekst til xmlkalender`() {
        val result = createXMLCalendarFromString("2016-01-01")
        assertNotNull(result)
        assertEquals("2016-01-01T00:00:00.000+01:00", result.toString())
        assertEquals("2016-01-01", result.simpleFormat())

    }

    @Test(expected = DateTimeParseException::class)
    fun `Test av konvertere datotekst til xmlkalender feiler`() {
        createXMLCalendarFromString("2016-Ø1-01")
    }
}
