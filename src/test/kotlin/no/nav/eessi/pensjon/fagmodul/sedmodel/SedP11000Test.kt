package no.nav.eessi.pensjon.fagmodul.sedmodel

import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class SedP11000Test {

    @Disabled
    @Test
    fun `compare SED P11000`() {
        val sedJson = getTestJsonFile("P11000_Fixed-NAV.json")

        val p11000sed = getSEDfromTestfile(sedJson)

        //TODO trenger å opprette nødvendige Objekter under NAV/Pensjon
        JSONAssert.assertEquals(sedJson, p11000sed.toJsonSkipEmpty(), false)
    }


}