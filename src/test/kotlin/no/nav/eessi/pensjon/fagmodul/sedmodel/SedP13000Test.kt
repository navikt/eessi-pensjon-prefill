package no.nav.eessi.pensjon.fagmodul.sedmodel

import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class SedP13000Test {

    @Disabled
    @Test
    fun `compare SED P13000`() {
        val sedJson = getTestJsonFile("P13000-NAV.json")

        val p13000sed = getSEDfromTestfile(sedJson)

        //TODO trenger å opprette nødvendige Objekter under NAV/Pensjon
        JSONAssert.assertEquals(sedJson, p13000sed.toJsonSkipEmpty(), false)
    }


}