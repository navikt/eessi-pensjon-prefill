package no.nav.eessi.eessifagmodul.utils

import org.junit.Test
import javax.xml.datatype.DatatypeFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UtilsTest {

    @Test
    fun `check SedEnum toString`() {

        val result = STANDARD_SED
        assertNotNull(result)
        assertEquals("P5000,P6000,P7000", result)

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
    fun `check variables containing SED`() {
        //val funlist = sedEnumToStringWidthSkip("P4000")
        val funlist = START_SED+","+STANDARD_SED
        assertEquals("P2000,P2100,P2200,P5000,P6000,P7000", funlist)

        val all = ALL_SED
        assertEquals("P2000,P2100,P2200,P4000,P6000,P5000,P7000", all)

        val start = START_SED
        assertEquals("P2000,P2100,P2200", start)

        val standard = STANDARD_SED
        assertEquals("P5000,P6000,P7000", standard)

        //assertEquals("P2000,P2100,P2200,P6000,P5000,P7000", STANDARD_SED)
        //assertEquals("P2000,P2100,P2200,P6000,P5000,P7000", START_SED)
        //assertEquals("P2000,P2100,P2200,P6000,P5000,P7000", ALL_SED)
    }


    @Test
    fun `playground testing`() {


    }

}