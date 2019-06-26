package no.nav.eessi.eessifagmodul.utils

import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SEDType
import no.nav.eessi.eessifagmodul.services.eux.bucmodel.ShortDocumentItem
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.util.UriComponentsBuilder
import java.time.format.DateTimeParseException
import javax.xml.datatype.DatatypeFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail


class UtilsTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(UtilsTest::class.java) }


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
        logger.info("XMLdata: $xmlcal   SimpleDate: $toRinaDate")

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

    @Test
    fun `check urlBuilder path correct`() {
        val path = "/fnr/"
        val id = "2342342342"

        val ekstra = "sakId"
        val ekstraval = "123412313"

        val uriBuilder = UriComponentsBuilder.fromPath(path).pathSegment(id)
        uriBuilder.queryParam(ekstra, ekstraval)

        assertEquals("/fnr/2342342342?sakId=123412313", uriBuilder.toUriString())

    }

    @Test
    fun `check for value on SEDtype`() {
        val px = "P3000"
        val result = SEDType.isValidSEDType(px)
        assertTrue(result)

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

    @Test
    fun `testing some kotling list filter stuff`() {
        val listdocs = listOf(ShortDocumentItem(id = "123123", type = "P2000", status = "done"),
                ShortDocumentItem(id = "234234", type = "P2200", status = "done")
        )
        listdocs.forEach {
            print(it)
        }
        val result = listdocs.map { it.id }.toList()
        logger.info("-------------------------------")
        logger.info(result.toString())
        logger.info("-------------------------------")


    }

    @Test
    fun `reverse map in kotlin`() {
        val status = mapOf<String, String>("01" to "UGIF", "02" to "GIFT", "03" to "SAMB", "04" to "REPA", "05" to "SKIL", "06" to "SKPA", "07" to "SEPA", "08" to "ENKE")

        val keys1 = status.keys
        keys1.forEach {
            print(" \"$it\" to \"${status.get(it)}\", ")
        }
        logger.info("---------------------------------------------------------------------------------------------")
        val rev = status.reversed()
        val keys = rev.keys
        keys.forEach {
            print(" \"$it\" to \"${rev.get(it)}\", ")
        }

        assertEquals("UGIF", status.get("01"))
        assertEquals("01", rev.get("UGIF"))

        assertEquals("ENKE", status.get("08"))
        assertEquals("08", rev.get("ENKE"))

    }

    @Test
    fun `call checkAndConvertInstituion med spesialtegn som input`() {

        val institusjonItem = InstitusjonItem(country = "NO", institution = "NO:NAVT002", name = null)
        val actual = checkAndConvertInstituion(institusjonItem)

        assertEquals(institusjonItem.institution, actual)

    }
    @Test
    fun `call checkAndConvertInstituion uten spesialtegn som input`() {

        val institusjonItem = InstitusjonItem(country = "NO", institution = "NAVT002", name = null)
        val actual = checkAndConvertInstituion(institusjonItem)

        assertEquals("NO:NAVT002", actual)
    }

    @Test
    fun `call checkAndConvertInstituion that is null as input`() {

        val institusjonItem = InstitusjonItem(country = "", institution = "", name = null)
        val actual = checkAndConvertInstituion(institusjonItem)

        assertEquals(":", actual)
    }

}