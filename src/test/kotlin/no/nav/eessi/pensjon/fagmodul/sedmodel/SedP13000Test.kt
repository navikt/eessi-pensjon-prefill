package no.nav.eessi.pensjon.fagmodul.sedmodel

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SedP13000Test {

    @Test
    fun `compare SED P13000`() {
        val sedJson = getTestJsonFile("P13000-NAV.json")
        val p13000sed = SED.fromJson(sedJson)

        Assertions.assertEquals(null, p13000sed.nav?.barnoppdragelse?.merknakder)
        Assertions.assertEquals("Reason for supplement suspension 6.5.2", p13000sed.pensjon?.pensjoninfotillegg?.opphoraarsak)
        Assertions.assertEquals("2300062", p13000sed.pensjon?.pensjoninfotillegg?.endret?.get(0)?.belopforendring)
    }
}