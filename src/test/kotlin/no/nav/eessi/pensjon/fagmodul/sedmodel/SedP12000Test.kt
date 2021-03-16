package no.nav.eessi.pensjon.fagmodul.sedmodel

import org.junit.jupiter.api.Test

class SedP12000Test {

    @Test
    fun `compare SED P12000`() {
        val sedJson = getTestJsonFile("P12000-NAV.json")
        SED.fromJson(sedJson)
    }
}