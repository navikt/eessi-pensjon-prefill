package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.trygdetid.V1Trygdetid
import no.nav.pensjon.v1.trygdetidliste.V1TrygdetidListe
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VedtakPensjonDataHelperTest {

    @Test
    fun `summerTrygdeTid forventet 0`() {

        val trygdetidListen = V1TrygdetidListe().apply {
            trygdetidListe.add(V1Trygdetid().apply {
                fom = 0.daysAgo()
                tom = 0.daysAhead()
            })
        }

        assertEquals(0, VedtakPensjonDataHelper.summerTrygdeTid(trygdetidListen))
    }

    @Test
    fun `summerTrygdeTid forventet 500 dager, erTrygdeTid forventet til false`() {

        val trygdetidListen = V1TrygdetidListe().apply {
            trygdetidListe.add(V1Trygdetid().apply {
                fom = 700.daysAgo()
                tom = 200.daysAgo()
            })
        }

        val result = VedtakPensjonDataHelper.summerTrygdeTid(trygdetidListen)

        assertEquals(500, result)

        val pendata = Pensjonsinformasjon().apply {
            trygdetidListe = trygdetidListen
        }

        //bod mye i utland mer en 360d.
        assertFalse(VedtakPensjonDataHelper.erTrygdeTid(pendata))
    }

    @Test
    fun `summerTrygdeTid forventet 15 dager, erTrygdeTid forventet til false`() {
        val trygdetidListen = PensjonsinformasjonMother.trePerioderMed5DagerHver()

        assertEquals(15, VedtakPensjonDataHelper.summerTrygdeTid(trygdetidListen))

        val pendata = Pensjonsinformasjon().apply {
            trygdetidListe = trygdetidListen
        }

        //bod for lite i utland mindre en 30 dager?
        assertFalse(VedtakPensjonDataHelper.erTrygdeTid(pendata))
    }

    @Test
    fun `summerTrygdeTid forventet 70 dager, erTrygdeTid forventet til true`() {
        val trygdetidListen = V1TrygdetidListe().apply {
            trygdetidListe.add(V1Trygdetid().apply {
                fom = 170.daysAgo()
                tom = 100.daysAgo()
            })
        }

        assertEquals(70, VedtakPensjonDataHelper.summerTrygdeTid(trygdetidListen))

        val pendata = Pensjonsinformasjon().apply {
            trygdetidListe = trygdetidListen
        }

        //bod i utland mindre en mer en 30 mindre en 360?
        assertTrue( VedtakPensjonDataHelper.erTrygdeTid(pendata))
    }

    @Test
    fun `summerTrygdeTid forventet 10 dager, erTrygdeTid forventet til false`() {
        val trygdetidListen = V1TrygdetidListe().apply {
            trygdetidListe.add(V1Trygdetid().apply {
                fom = 50.daysAgo()
                tom = 40.daysAgo()
            })
        }

        assertEquals(10, VedtakPensjonDataHelper.summerTrygdeTid(trygdetidListen))

        val pendata = Pensjonsinformasjon().apply {
            trygdetidListe = trygdetidListen
        }

        //bod i utland mindre totalt 10dager en mer en mindre en 30 og mindre en 360
        assertFalse(VedtakPensjonDataHelper.erTrygdeTid(pendata))
    }

}
