package no.nav.eessi.pensjon.eux.model

import no.nav.eessi.pensjon.eux.model.sed.SED
import org.junit.jupiter.api.Test

class SedP2000Test {

    @Test
    fun `create SED P2000 from json datafile`() {
        val p2000json = javaClass.getResource("json/nav/P2000-NAV.json")?.readText()
        if (p2000json != null) {
            SED.fromJson(p2000json)
        }
    }

    @Test
    fun `create SED P2000 new v4_1 from json datafile`() {
        val p2000json = javaClass.getResource("/json/nav/P2000-NAV-4.1-new.json")!!.readText()
        SED.fromJson(p2000json)
    }

}
