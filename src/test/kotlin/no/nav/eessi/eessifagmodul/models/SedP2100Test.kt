package no.nav.eessi.eessifagmodul.models

import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SedP2100Test : AbstractSedTest() {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP2100Test::class.java) }

    @Test
    fun `create SED P2100 from json datafile`() {

        val p2100json = getTestJsonFile("P2100-NAV.json")
        val p2100sed = getSEDfromTestfile(p2100json)

        val json = p2100sed.toJson()
        JSONAssert.assertEquals(p2100json, json, false)
    }
}
