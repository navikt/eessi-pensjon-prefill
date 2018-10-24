package no.nav.eessi.eessifagmodul.utils

import org.junit.Test
import javax.xml.datatype.DatatypeFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UtilsTest {

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
        println("XMLdata: $xmlcal   SimpleDate: $toRinaDate")

        assertNotNull(toRinaDate)
        assertEquals("2016-01-01", toRinaDate)
    }
}