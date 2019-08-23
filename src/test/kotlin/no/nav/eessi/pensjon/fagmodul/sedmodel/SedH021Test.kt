package no.nav.eessi.pensjon.fagmodul.sedmodel

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class SedH021Test {

    @Test
    fun `compare SED H021 from json datafile`() {

        val h021json = getTestJsonFile("horisontal/H021-NAV.json")
        val h021sed = getHSEDfromTestfile(h021json)

        JSONAssert.assertEquals(h021json, h021sed.toString(), false)
    }
}
