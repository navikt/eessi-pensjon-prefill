package no.nav.eessi.pensjon.fagmodul.sedmodel

import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SedP5000Test {

    @Test
    fun `validate P5000 to json and back`() {
        val navSedP5000 = SedMock().genererP5000Mock()
        assertNotNull(navSedP5000)

        val json = mapAnyToJson(navSedP5000, true)
        val pensjondata = mapJsonToAny(json, typeRefs<SED>())
        assertNotNull(pensjondata)
        assertEquals(navSedP5000, pensjondata)
    }

    @Test
    fun `validate for p5000 sed in buc`() {
        val sedOne = SED.fromJson(getTestJsonFile("/p5000/P5000a_898403-nav.json"))
        assertNotNull(sedOne.pensjon?.medlemskap)

        val sedTwo = SED.fromJson(getTestJsonFile("/p5000/P5000b_898403-nav.json"))
        assertNotNull(sedTwo.pensjon?.medlemskap)

        val sedTree = SED.fromJson(getTestJsonFile("/p5000/P5000c_898403-nav.json"))
        assertNotNull(sedTree.pensjon?.medlemskap)

        val sedFour = SED.fromJson(getTestJsonFile("/p5000/P5000d_898403-nav.json"))
        assertNotNull(sedFour.pensjon?.medlemskap)

    }

}
