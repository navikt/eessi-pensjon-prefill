package no.nav.eessi.pensjon.prefill.sed.vedtak.helper

import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.sed.vedtak.PensjonsinformasjonMother
import no.nav.eessi.pensjon.prefill.sed.vedtak.daysAgo
import no.nav.eessi.pensjon.prefill.sed.vedtak.daysAhead
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjonsinformasjonClientMother.fraFil
import no.nav.eessi.pensjon.utils.simpleFormat
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.trygdetid.V1Trygdetid
import no.nav.pensjon.v1.trygdetidliste.V1TrygdetidListe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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

    @Test
    fun `forventer 13482 dager i sum summerTrygdeTid`() {
        val dataFromPESYS = PensjonsinformasjonService(fraFil("P6000-APUtland-301.xml"), "q")

        val pendata = dataFromPESYS.hentMedVedtak("someVedtakId")

        assertTrue( 13400 < VedtakPensjonDataHelper.summerTrygdeTid(pendata.trygdetidListe))
    }

    @Test
    fun `forventer at ytelseprMaaned er siste i listen`() {
        val dataFromPESYS = PensjonsinformasjonService(fraFil("P6000-UT-220.xml"), "q")
        val pendata = dataFromPESYS.hentMedVedtak("someVedtakId")

        val sisteprmnd = VedtakPensjonDataHelper.hentSisteYtelsePerMaaned(pendata)

        assertEquals("2017-05-01", sisteprmnd?.fom?.simpleFormat())
        assertEquals("7191", sisteprmnd?.belop.toString())
        assertEquals(false, VedtakPensjonDataHelper.isMottarMinstePensjonsniva(pendata))
        assertEquals("7191", VedtakPensjonDataHelper.hentYtelseBelop(pendata))

        assertEquals("EOS", VedtakPensjonDataHelper.hentVinnendeBergeningsMetode(pendata))
    }
}
