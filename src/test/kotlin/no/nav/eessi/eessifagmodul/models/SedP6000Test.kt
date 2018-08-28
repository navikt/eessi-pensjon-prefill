package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs
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
class SedP6000Test {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP6000Test::class.java) }

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
        val pensjondata = mapJsonToAny(json, typeRefs<SED>())
        assertNotNull(pensjondata)
        assertEquals(sed6000, pensjondata)

        //map load P6000-NAV refrence
        val path = Paths.get("src/test/resources/json/P6000-NAV.json")
        val p6000file = String(Files.readAllBytes(path))
        assertNotNull(p6000file)
        validateJson(p6000file)

        //map P6000-NAV back to P6000 object.
        val pensjondataFile = mapJsonToAny(p6000file, typeRefs<SED>())
        assertNotNull(pensjondataFile)

        //map P6000-NAV obj back to json
        val jsonnav = mapAnyToJson(sed6000)

        println("------------------generated----------------------")
        println("\n\n $json \n\n")
        println("------------------p6000-nav----------------------")
        println("\n\n $jsonnav \n\n")
        println("-------------------------------------------------")

    }

    @Test
    fun `create part json to object`() {
        val sed6000 = SedMock().genererP6000Mock()
        assertNotNull(sed6000)

        //hente ut bruker
        val bruker = sed6000.nav!!.bruker!!
        //map bruker til json så tilbake til brukerback
        val brukerback = mapJsonToAny(mapAnyToJson(bruker), typeRefs<Bruker>())
        //alt ok?
        assertNotNull(brukerback)
        assertEquals(bruker, brukerback)

        val sed = createSED("P6000")
        val navmock = NavMock().genererNavMock()
        sed.nav = Nav(
                bruker = navmock.bruker
        )
        val penmock = PensjonMock().genererMockData()
        sed.pensjon = Pensjon(
                gjenlevende = penmock.gjenlevende
        )
        val testPersjson = mapAnyToJson(sed, true)

        println("------------------generated----------------------")
        println("\n\n $testPersjson \n\n")
        println("------------------p6000-nav----------------------")

    }

    @Test
    fun `check for valid json to object`() {
        val test = "{\"postnummer\":\"sdafsdaf\",\"by\":\"asfdsdaf\",\"land\":\"BG\",\"gate\":\"sdfasd\",\"bygning\":\"sdfsdf\","
        val result = validateJson(test)
        assertNotNull(result)
        assertEquals(false, result)
    }
}