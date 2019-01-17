package no.nav.eessi.eessifagmodul.models

import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals

class SedP3000noTest : AbstractSedTest() {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP3000noTest::class.java) }

    @Test
    fun `create SED P3000_NO from json datafile`() {
        val p3000json = getTestJsonFile("P3000_NO-NAV.json")
        val p3000sed = getSEDfromTestfile(p3000json)

        val json = p3000sed.toJson()
        JSONAssert.assertEquals(p3000json, json, false)

        assertEquals("6511", p3000sed.pensjon?.landspesifikk?.norge?.ufore?.barnInfo!!.get(0).etternavn)
        assertEquals("CZK", p3000sed.pensjon?.landspesifikk?.norge?.alderspensjon?.ektefelleInfo?.pensjonsmottaker!!.first().institusjonsopphold?.belop?.last()!!.valuta)
    }
}
