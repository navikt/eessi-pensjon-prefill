package no.nav.eessi.eessifagmodul.models

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BUCTest {

    @Test
    fun testNormalBUC() {
        val buc = BUC(
                flytType = "P_BUC_01",
                saksnummerPensjon = "101",
                saksbehandler = "202",
                Parter = SenderReceiver(
                        sender = Institusjon(landkode = "NO", navn = "NAV"),
                        receiver = listOf(Institusjon(landkode = "DK", navn = "ATP"))
                ),
                NAVSaksnummer =  "nav_saksnummer",
                SEDType = "SED_type",
                notat_tmp = "Temp"
        )

        assertEquals(buc.flytType, "P_BUC_01")
        assertEquals(buc.saksnummerPensjon, "101")
        assertEquals(buc.saksbehandler, "202")
        assertNotNull(buc.Parter)
        assertEquals(buc.SEDType, "SED_type")
        assertEquals(buc.NAVSaksnummer, "nav_saksnummer")
        assertEquals(buc.notat_tmp, "Temp")
    }

}