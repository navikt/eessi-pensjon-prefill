package no.nav.eessi.pensjon.fagmodul.models

import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals

class SedH021Test  : AbstractSedTest() {

    private val logger = LoggerFactory.getLogger(SedH021Test::class.java)

    @Test
    fun `compare SED H021 from json datafile`() {

        val h021json = getTestJsonFile("horisontal/H021-NAV.json")
        val h021sed = getHSEDfromTestfile(h021json)

        JSONAssert.assertEquals(h021json, h021sed.toString(), false)

    }

}