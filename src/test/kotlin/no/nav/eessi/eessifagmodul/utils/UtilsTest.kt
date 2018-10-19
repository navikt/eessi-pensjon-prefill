package no.nav.eessi.eessifagmodul.utils

import no.nav.eessi.eessifagmodul.models.Bruker
import no.nav.eessi.eessifagmodul.models.Nav
import org.junit.Test
import javax.xml.datatype.DatatypeFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UtilsTest {

    @Test
    fun `check SedEnum toString`() {

        val result = STANDARD_SED
        assertNotNull(result)
        assertEquals("P3000,P5000,vedtak,P7000", result)

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
        assertEquals("P2000,P2100,P2200,P3000,P5000,vedtak,P7000", funlist)

        val all = ALL_SED
        assertEquals("P2000,P2100,P2200,P3000,P4000,vedtak,P5000,P7000", all)

        val start = START_SED
        assertEquals("P2000,P2100,P2200", start)

        val standard = STANDARD_SED
        assertEquals("P3000,P5000,vedtak,P7000", standard)
    }

    @Test
    fun `playground testing`() {
        val tst = "{\n" +
                "    \"ektefelle\" : {\n" +
                "\t  \"_key\" : \"345345345--3453455-345-34534534535345\",\n" +
                "      \"person\" : {\n" +
                "        \"pin\" : [ {\n" +
                "          \"land\" : \"BE\",\n" +
                "          \"identifikator\" : \"7979789789\",\n" +
                "          \"sektor\" : \"dagpenger\"\n" +
                "        }, {\n" +
                "          \"land\" : \"HR\",\n" +
                "          \"identifikator\" : \"789978\",\n" +
                "          \"sektor\" : \"sykdom\"\n" +
                "        } ],\n" +
                "        \"etternavnvedfoedsel\" : \"jkkljkl\",\n" +
                "        \"fornavn\" : \"jkhkjhjk\",\n" +
                "        \"tidligereetternavn\" : \"ølkølk\",\n" +
                "        \"foedested\" : {\n" +
                "          \"land\" : \"BE\",\n" +
                "          \"by\" : \"jlkjlj\",\n" +
                "          \"region\" : \"kljkljljk\"\n" +
                "        },\n" +
                "        \"statsborgerskap\" : [ {\n" +
                "          \"land\" : \"BE\"\n" +
                "        }, {\n" +
                "          \"land\" : \"HR\"\n" +
                "        } ],\n" +
                "        \"etternavn\" : \"jhhjkh\",\n" +
                "        \"foedselsdato\" : \"1999-04-01\",\n" +
                "        \"tidligerefornavn\" : \"ølkøl\",\n" +
                "        \"kjoenn\" : \"f\",\n" +
                "        \"fornavnvedfoedsel\" : \"jkljkljl\"\n" +
                "      },\n" +
                "      \"type\" : \"ektefelle\",\n" +
                "      \"far\" : {\n" +
                "        \"person\" : {\n" +
                "          \"etternavnvedfoedsel\" : \"ølkøk\",\n" +
                "          \"fornavn\" : \"øklø\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"mor\" : {\n" +
                "        \"person\" : {\n" +
                "          \"etternavnvedfoedsel\" : \"køølkøl\",\n" +
                "          \"fornavn\" : \"jøjøkj\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "}"


        val nav = mapJsonToAny(tst, typeRefs<Nav>())

        val navJson = mapAnyToJson(nav)
        print("bruker: $navJson")

    }

}