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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SedP2200Test {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP2200Test::class.java) }

    private val printout : Boolean = false

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
    }


    @Test
    fun `create SED P2200 from json datafile`() {

        val p2200path = Paths.get("src/test/resources/json/P2200-NAV.json")
        val p2200file = String(Files.readAllBytes(p2200path))

        assertTrue(validateJson(p2200file))

        if (printout) {
            println("--------------------------------------------------------------------------------------------")
            println(p2200file)
            println("--------------------------------------------------------------------------------------------")
        }
        val p2200sed = mapJsonToAny(p2200file, typeRefs<SED>(), true)

        assertNotNull(p2200sed)
        assertEquals(SED::class.java, p2200sed::class.java)

    }

    @Test
    fun `create SED P2200 from json datafile and backagain`() {

        val p2200path = Paths.get("src/test/resources/json/P2200-NAV.json")
        val p2200file = String(Files.readAllBytes(p2200path))

        assertTrue(validateJson(p2200file))

        if (printout) {
            println("--------------------------------------------------------------------------------------------")
            println(p2200file)
            println("--------------------------------------------------------------------------------------------")
        }
        val p2200sed = mapJsonToAny(p2200file, typeRefs<SED>(), true)

        assertNotNull(p2200sed)
        val json = mapAnyToJson(p2200sed, true)

        if (printout) {
            println("--------------------------------------------------------------------------------------------")
            println(json)
            println("--------------------------------------------------------------------------------------------")
        }
        JSONAssert.assertEquals(p2200file, json, false)

    }


}