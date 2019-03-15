package no.nav.eessi.eessifagmodul.models

import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SedP2000Test : AbstractSedTest() {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP2000Test::class.java) }

    @Test
    fun `create SED P2000 from json datafile`() {

        val p2000json = getTestJsonFile("P2000-NAV.json")
        val p2000sed = getSEDfromTestfile(p2000json)

        val json = p2000sed.toJson()
        JSONAssert.assertEquals(p2000json, json, false)
    }

    @Test
    fun `create SED P2000 new v4_1 from json datafile`() {

        val p2000json = getTestJsonFile("P2000-NAV-4.1-new.json")
        val p2000sed = getSEDfromTestfile(p2000json)

        val json = p2000sed.toJson()
        JSONAssert.assertEquals(p2000json, json, false)

    }

}
