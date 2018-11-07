package no.nav.eessi.eessifagmodul.utils

import org.junit.Test
import javax.xml.datatype.DatatypeFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

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

    @Test
    fun `playground testing`() {
        val barnSEDlist = listOf<String>("P2000", "P2100", "P2200")

        if (barnSEDlist.contains("P6000").not()) {
            assertTrue(true, "Yes")
        } else {
            fail("no")
        }

        if (barnSEDlist.contains("P2000").not()) {
            fail("no")
        } else {
            assertTrue(true, "Yes")
        }


    }

}