package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.utils.validateJson
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertTrue

class SedP2000Test {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP2000Test::class.java) }

    @Test
    fun `create SED P2000 from json datafile`() {
        val p2000path = Paths.get("src/test/resources/json/P2000-NAV.json")
        val p2000file = String(Files.readAllBytes(p2000path))
        assertTrue(validateJson(p2000file))

        val p2000sed = SED.fromJson(p2000file)
        val json = p2000sed.toJson()
        JSONAssert.assertEquals(p2000file, json, false)
    }
}
