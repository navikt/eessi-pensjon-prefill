package no.nav.eessi.pensjon.fagmodul.sedmodel

import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class SedP12000Test {

    @Disabled
    @Test
    fun `compare SED P12000`() {
        val sedJson = getTestJsonFile("P12000-NAV.json")

        val p12000sed = getSEDfromTestfile(sedJson)

        //TODO trenger å opprette nødvendige Objekter under NAV/Pensjon
        JSONAssert.assertEquals(sedJson, p12000sed.toJsonSkipEmpty(), false)
    }


}