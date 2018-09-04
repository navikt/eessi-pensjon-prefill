package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs
import no.nav.eessi.eessifagmodul.utils.validateJson
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SedP2000Test {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP2000Test::class.java) }

    private val printout : Boolean = false

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
    }


    @Test
    fun `create SED P2000 from json datafile`() {

        val p2000path = Paths.get("src/test/resources/json/P2000-NAV.json")
        val p2000file = String(Files.readAllBytes(p2000path))

        assertTrue(validateJson(p2000file))

        if (printout) {
            println("--------------------------------------------------------------------------------------------")
            println(p2000file)
            println("--------------------------------------------------------------------------------------------")
        }

        val p2000sed = mapJsonToAny(p2000file, typeRefs<SED>(), true)

        assertNotNull(p2000sed)

        val json = mapAnyToJson(p2000sed, true)

        if (printout) {
            println("--------------------------------------------------------------------------------------------")
            println(json)
            println("--------------------------------------------------------------------------------------------")
        }
        JSONAssert.assertEquals(p2000file, json, false)
    }


}
