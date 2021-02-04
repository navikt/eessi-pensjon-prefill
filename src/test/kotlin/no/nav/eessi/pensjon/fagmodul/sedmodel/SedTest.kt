package no.nav.eessi.pensjon.fagmodul.sedmodel

import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.utils.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class SedTest {

    @Test
    fun createP6000sed() {
        val sed6000 = SedMock().genererP6000Mock()
        assertNotNull(sed6000)

        val json = sed6000.toJson()
        //map json back to vedtak obj
        val pensjondata = SED.fromJson(json)
        assertNotNull(pensjondata)
        assertEquals(sed6000, pensjondata)

        //map load vedtak-NAV refrence
        val path = Paths.get("src/test/resources/json/nav/P6000-NAV.json")
        val p6000file = String(Files.readAllBytes(path))
        assertNotNull(p6000file)
        validateJson(p6000file)

        //map vedtak-NAV back to vedtak object.
        val pensjondataFile = SED.fromJson(p6000file)
        assertNotNull(pensjondataFile)

    }

    @Test
    fun `create part json to object`() {
        val sed6000 = SedMock().genererP6000Mock()
        assertNotNull(sed6000)

        val bruker = sed6000.nav!!.bruker!!
        val brukerback = mapJsonToAny(mapAnyToJson(bruker), typeRefs<Bruker>())
        assertNotNull(brukerback)
        assertEquals(bruker, brukerback)


        val navmock = NavMock().genererNavMock()
        val penmock = PensjonMock().genererMockData()

        val sed = SED(
                type = SEDType.P6000,
                nav = Nav(bruker = navmock.bruker),
                pensjon = Pensjon(gjenlevende = penmock.gjenlevende)
        )

        val testPersjson = mapAnyToJson(sed, true)
        assertNotNull(testPersjson)

    }

}
