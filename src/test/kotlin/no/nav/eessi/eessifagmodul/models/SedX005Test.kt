package no.nav.eessi.eessifagmodul.models

import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals

class SedX005Test : AbstractSedTest() {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP5000Test::class.java) }

    @Test
    fun `compare SED X005 to X005 from json datafile`() {

        val x005json = getTestJsonFile("X005-NAV.json")
        val x005sed = getSEDfromTestfile(x005json)

        assertEquals("Duck", x005sed.nav?.sak?.kontekst?.bruker?.person?.etternavn)
        assertEquals("Dummy", x005sed.nav?.sak?.kontekst?.bruker?.person?.fornavn)
        assertEquals("1958-02-01", x005sed.nav?.sak?.kontekst?.bruker?.person?.foedselsdato)

        assertEquals("NO:NAVT002", x005sed.nav?.sak?.leggtilinstitusjon?.institusjon?.id)
        assertEquals("NAVT002", x005sed.nav?.sak?.leggtilinstitusjon?.institusjon?.navn)

        val json = x005sed.toJsonSkipEmpty()
        JSONAssert.assertEquals(x005json, json, false)


        val xprefill005json = getTestJsonFile("PrefillX005-NAV.json")
        val xprefill005sed = getSEDfromTestfile(xprefill005json)

        assertEquals("POTET", xprefill005sed.nav?.sak?.kontekst?.bruker?.person?.etternavn)
        assertEquals("KRIMINELL", xprefill005sed.nav?.sak?.kontekst?.bruker?.person?.fornavn)
        assertEquals("1944-12-25", xprefill005sed.nav?.sak?.kontekst?.bruker?.person?.foedselsdato)

        assertEquals("NO:NAVT007", xprefill005sed.nav?.sak?.leggtilinstitusjon?.institusjon?.id)
        assertEquals("NAVT007", xprefill005sed.nav?.sak?.leggtilinstitusjon?.institusjon?.navn)

        val json2 = xprefill005sed.toJson()
        JSONAssert.assertEquals(xprefill005json, json2, true)


    }
}