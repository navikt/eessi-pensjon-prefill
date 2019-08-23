package no.nav.eessi.pensjon.fagmodul.sedmodel

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class SedP2100Test {

    @Test
    fun `create SED P2100 from json datafile`() {

        val p2100json = getTestJsonFile("P2100-NAV.json")
        val p2100sed = getSEDfromTestfile(p2100json)

        val json = p2100sed.toJson()
        JSONAssert.assertEquals(p2100json, json, false)
    }
}
