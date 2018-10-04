package no.nav.eessi.eessifagmodul.utils

import org.junit.Test
import javax.xml.datatype.DatatypeFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UtilsTest {

    @Test
    fun `check SedEnum toString`() {

        val result = sedEnumToString()
        assertNotNull(result)
        assertEquals("P2000,P2100,P2200,P4000,P6000,P5000,P7000", result)

    }

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
    fun `playground testing`() {




    }

}