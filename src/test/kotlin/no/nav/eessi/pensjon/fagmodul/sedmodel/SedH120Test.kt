package no.nav.eessi.pensjon.fagmodul.sedmodel

import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals

class SedH120Test  : AbstractSedTest() {

    private val logger = LoggerFactory.getLogger(SedH120Test::class.java)

    @Test
    fun `compare SED H120 from json datafile`() {

        val h120json = getTestJsonFile("horisontal/H120-NAV.json")
        val h120sed = getSEDfromTestfile(h120json)



        val startskap = h120sed.nav?.bruker?.person?.statsborgerskap
        assertEquals(3, startskap?.size)

        JSONAssert.assertEquals(h120json, h120sed.toJsonSkipEmpty(), false)

    }

}