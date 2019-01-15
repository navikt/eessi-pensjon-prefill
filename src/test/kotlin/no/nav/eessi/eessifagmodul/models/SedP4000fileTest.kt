package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs
import no.nav.eessi.eessifagmodul.utils.validateJson
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SedP4000fileTest {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP4000fileTest::class.java) }

    @Test
    fun `validate P4000 to json and back`() {
        val p4000path = Paths.get("src/test/resources/json/P4000-NAV.json")
        val p4000file = String(Files.readAllBytes(p4000path))
        assertTrue(validateJson(p4000file))
        val p4000sed = SED.fromJson(p4000file)

        val json = mapAnyToJson(p4000sed, true)
        val pensjondata = mapJsonToAny(json, typeRefs<SED>())
        assertNotNull(pensjondata)

        println(p4000file)

        println(pensjondata)

        JSONAssert.assertEquals(p4000file, json, false)
    }
}