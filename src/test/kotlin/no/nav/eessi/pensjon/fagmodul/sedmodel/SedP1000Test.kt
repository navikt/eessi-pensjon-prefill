package no.nav.eessi.pensjon.fagmodul.sedmodel

import org.junit.jupiter.api.Test

class SedP1000Test {

    @Test
    fun `compare SED P1000`() {
        val sedJson = getTestJsonFile("P1000-NAV.json")
        SED.fromJson(sedJson)
    }


}