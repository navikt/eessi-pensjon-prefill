package no.nav.eessi.eessifagmodul.models

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.validateJson
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class SedTest{

    val logger: Logger by lazy { LoggerFactory.getLogger(SedTest::class.java) }

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        MockitoAnnotations.initMocks(this)
    }


    @Test
    fun createP6000sed() {
        val sed6000 = SedMock().genererP6000Mock()
        assertNotNull(sed6000)

        val json = mapAnyToJson(sed6000)

        //map json back to P6000 obj
        val map = jacksonObjectMapper()
        val pensjondata = map.readValue(json, SED::class.java)
        assertNotNull(pensjondata)
        assertEquals(sed6000, pensjondata)

        //map load P6000-NAV refrence
        val path = Paths.get("src/test/resources/json/P6000-NAV.json")
        val p6000file = String(Files.readAllBytes(path))
        assertNotNull(p6000file)
        validateJson(p6000file)

        //map P6000-NAV back to P6000 object.
        val pensjondataFile= jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(p6000file, SED::class.java)
        assertNotNull(pensjondataFile)

        //map P6000-NAV obj back to json
        val jsonnav = mapAnyToJson(sed6000)

        println("------------------generated----------------------")
        println("\n\n $json \n\n")
        println("------------------p6000-nav----------------------")
        println("\n\n $jsonnav \n\n")
        println("-------------------------------------------------")

    }

}