package no.nav.eessi.pensjon.eux.model

import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
@Disabled
class SedP2200Test {

    lateinit var p2200json: String
    lateinit var p2200sed: SED

    @BeforeEach
    fun bringItOn() {
        p2200json = getTestJsonFile("P2000-NAV.json")
        p2200sed = SED.fromJson(p2200json)
    }

    @Test
    fun `create SED P2200 from mockData`() {

        val p2200 =  SedMock().genererP2000Mock()
        assertNotNull(p2200)

        val p2200json = mapAnyToJson(p2200, true)
        assertNotNull(p2200json)

        mapJsonToAny<SED>(p2200json)
    }

    @Test
    fun `create SED P2200 from json datafile`() {

        val p2200sed = mapJsonToAny<SED>(p2200json, true)
        assertNotNull(p2200sed)
        assertEquals(SED::class.java, p2200sed::class.java)
        mapAnyToJson(p2200sed, true)
    }
}
