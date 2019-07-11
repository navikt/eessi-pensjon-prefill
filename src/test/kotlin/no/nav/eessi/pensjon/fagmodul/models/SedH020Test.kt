package no.nav.eessi.pensjon.fagmodul.models

import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals

class SedH020Test  : AbstractSedTest() {

    private val logger = LoggerFactory.getLogger(SedH020Test::class.java)

    @Test
    fun `compare SED H020 from json datafile`() {

        val h020json = getTestJsonFile("horisontal/H020-NAV.json")
        val h020sed = getHSEDfromTestfile(h020json)

        JSONAssert.assertEquals(h020json, h020sed.toString(), false)

    }

}