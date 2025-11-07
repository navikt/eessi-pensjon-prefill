package no.nav.eessi.pensjon.eux.model

import no.nav.eessi.pensjon.eux.model.sed.P5000
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class SedP5000Test {

    @Test
    fun `validate P5000 to json and back`() {
        val navSedP5000 = SedMock().genererP5000Mock()
        assertNotNull(navSedP5000)

        val json = mapAnyToJson(navSedP5000, true)
        val pensjondata = mapJsonToAny<SED>(json)
        assertNotNull(pensjondata)
    }

    @Test
    fun `Gitt en P5000 så map trygdetidsperioder`() {
        val p5000json = String(Files.readAllBytes(Paths.get("src/test/resources/json/nav/P5000-NAV.json")))
        val p5000 = mapJsonToAny<P5000>(p5000json)

        assertNotNull(p5000.pensjon?.trygdetid)
    }

}
