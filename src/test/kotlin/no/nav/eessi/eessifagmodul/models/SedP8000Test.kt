package no.nav.eessi.eessifagmodul.models

import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals


class SedP8000Test : AbstractSedTest() {
    val logger: Logger by lazy { LoggerFactory.getLogger(SedP8000Test::class.java) }

    @Test
    fun `create SED P8000 from json datafile`() {

        val p8000json = getTestJsonFile("P8000-NAV.json")
        val p8000sed = getSEDfromTestfile(p8000json)

        val json = p8000sed.toJson()
        JSONAssert.assertEquals(p8000json, json, false)

        assertEquals("Her kommer tekst ad Begrunnelse for kravet", p8000sed.pensjon?.anmodning?.informasjon?.begrunnelseKrav)

        assertEquals("Begrunnelse for kravet", p8000sed.pensjon?.anmodning?.begrunnKrav)



    }
}