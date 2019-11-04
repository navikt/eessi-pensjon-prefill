package no.nav.eessi.pensjon.fagmodul.sedmodel

import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class SedP1100Test {

    @Disabled
    @Test
    fun `compare SED P1100`() {
        val sedJson = getTestJsonFile("P1100-NAV.json")

        val p1100sed = getSEDfromTestfile(sedJson)

        //TODO trenger å opprette nødvendige Objekter under NAV/Pensjon
        JSONAssert.assertEquals(sedJson, p1100sed.toJsonSkipEmpty(), false)
    }


}