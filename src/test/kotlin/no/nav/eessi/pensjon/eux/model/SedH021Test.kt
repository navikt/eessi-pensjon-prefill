package no.nav.eessi.pensjon.eux.model

import no.nav.eessi.pensjon.eux.model.sed.SED
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SedH021Test {

    @Test
    fun `compare SED H021 from json datafile`() {
        val h021json = getTestJsonFile("horisontal/H021-A-NAV.json")
        val h021sed = SED.fromJson(h021json)

        Assertions.assertNotNull(h021sed.nav?.bruker?.person?.pinland)
        Assertions.assertEquals("213421412414214", h021sed.nav?.bruker?.person?.pinland?.kompetenteuland)
    }

}
