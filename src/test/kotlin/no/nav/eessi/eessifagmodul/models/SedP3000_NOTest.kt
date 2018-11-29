package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.utils.validateJson
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SedP3000_NOTest {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP3000_NOTest::class.java) }

    @Test
    fun `create SED P3000_NO from json datafile`() {
        val p3000path = Paths.get("src/test/resources/json/P3000_NO-NAV.json")
        val p3000file = String(Files.readAllBytes(p3000path))
        assertTrue(validateJson(p3000file))

        val p3000sed = SED.fromJson(p3000file)
        val json = p3000sed.toJson()
        JSONAssert.assertEquals(p3000file, json, false)

        assertEquals("6511", p3000sed.pensjon?.landspesifikk?.norge?.ufore?.barnInfo!!.get(0).etternavn)
        assertEquals("CZK", p3000sed.pensjon?.landspesifikk?.norge?.alderspensjon?.ektefelleInfo?.pensjonsmottaker!!.first().institusjonsopphold?.belop?.last()!!.valuta)
    }
}
