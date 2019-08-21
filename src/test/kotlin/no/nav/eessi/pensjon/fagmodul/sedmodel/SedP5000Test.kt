package no.nav.eessi.pensjon.fagmodul.sedmodel

import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

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
}
