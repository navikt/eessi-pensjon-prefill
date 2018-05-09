package no.nav.eessi.eessifagmodul.models

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(JUnit4::class)
class BUCTest {

    @Test
    fun testNormalBUC() {
        val parter = SenderReceiver(
                sender = Institusjon(landkode = "NO", navn = "NAV"),
                receiver = listOf(Institusjon(landkode = "DK", navn = "ATP"))
        )

        val buc = BUC(
                flytType = "P_BUC_01",
                saksnummerPensjon = "101",
                saksbehandler = "202",
                Parter = parter,
                NAVSaksnummer =  "nav_saksnummer",
                SEDType = "SED_type",
                notat_tmp = "Temp"
        )

        assertEquals(buc.flytType, "P_BUC_01")
        assertEquals(buc.saksnummerPensjon, "101")
        assertEquals(buc.saksbehandler, "202")
        assertEquals(buc.SEDType, "SED_type")
        assertEquals(buc.NAVSaksnummer, "nav_saksnummer")
        assertEquals(buc.notat_tmp, "Temp")

        assertNotNull(buc.Parter)
        assertEquals("NO", parter.sender.landkode)
        assertEquals("NAV", parter.sender.navn)
    }

}