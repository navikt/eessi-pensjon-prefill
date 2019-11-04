package no.nav.eessi.pensjon.fagmodul.sedmodel

import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class SedP1000Test {

    @Disabled
    @Test
    fun `compare SED P1000`() {
        val sedJson = getTestJsonFile("P1000-NAV.json")

        val p1000sed = getSEDfromTestfile(sedJson)

        //TODO trenger å opprette nødvendige Objekter under NAV/Pensjon
        JSONAssert.assertEquals(sedJson, p1000sed.toJsonSkipEmpty(), false)
    }


}