package no.nav.eessi.pensjon.fagmodul.sedmodel

import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.junit.Assert.assertNotNull

class SedP4000fileTest {

    @Test
    fun `validate P4000 to json and back`() {
        val p4000json = getTestJsonFile("P4000-NAV.json")
        val p4000sed = getSEDfromTestfile(p4000json)

        val json = mapAnyToJson(p4000sed, true)
        val pensjondata = mapJsonToAny(json, typeRefs<SED>())
        assertNotNull(pensjondata)
        JSONAssert.assertEquals(p4000json, json, false)
    }
}
