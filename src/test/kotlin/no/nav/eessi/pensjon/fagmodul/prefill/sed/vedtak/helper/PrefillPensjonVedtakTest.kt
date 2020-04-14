package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.helper

import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.hjelper.PrefillPensjonVedtak
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjonsinformasjonClientMother.fraFil
import no.nav.eessi.pensjon.utils.mapAnyToJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrefillPensjonVedtakTest {

    @Test
    fun `forventet createVedtakTypePensionWithRule verdi ALDER`() {
        val dataFromPESYS = PensjonsinformasjonHjelper(fraFil("P6000-APUtland-301.xml"))

        val pendata = dataFromPESYS.hentMedVedtak("someVedtakId")

        assertEquals("01", PrefillPensjonVedtak.createVedtakTypePensionWithRule(pendata))
    }

    @Test
    fun `forventet createVedtakTypePensionWithRule verdi GJENLEVENDE`() {
        val dataFromPESYS = PensjonsinformasjonHjelper(fraFil("P6000-GP-401.xml"))
        val pendata = dataFromPESYS.hentMedVedtak("someVedtakId")

        assertEquals("03", PrefillPensjonVedtak.createVedtakTypePensionWithRule(pendata))
    }

    @Test
    fun `forventet createVedtakTypePensionWithRule verdi UFØRE`() {
        val dataFromPESYS = PensjonsinformasjonHjelper(fraFil("P6000-UT-201.xml"))
        val pendata = dataFromPESYS.hentMedVedtak("someVedtakId")

        assertEquals("02", PrefillPensjonVedtak.createVedtakTypePensionWithRule(pendata))
    }

    @Test
    fun `forventet korrekt utregnet ytelsePrMnd på Uforep hvor UT_ORDINER`() {
        val dataFromPESYS = PensjonsinformasjonHjelper(fraFil("P6000-UT-220.xml"))
        val pendata = dataFromPESYS.hentMedVedtak("someVedtakId")

        val result = PrefillPensjonVedtak.createBeregningItemList(pendata)

        mapAnyToJson(result, true)

        assertEquals(6, result.size)

        val ytelsePerMaaned1 = result[0]
        assertEquals("6917", ytelsePerMaaned1.beloepBrutto?.beloep)
        assertEquals("2015-12-01", ytelsePerMaaned1.periode?.fom)
        assertEquals("2015-12-31", ytelsePerMaaned1.periode?.tom)

        val ytelsePerMaaned2 = result[1]
        assertEquals("6917", ytelsePerMaaned2.beloepBrutto?.beloep)
        assertEquals("2016-01-01", ytelsePerMaaned2.periode?.fom)
        assertEquals("2016-04-30", ytelsePerMaaned2.periode?.tom)

        val ytelsePerMaaned3 = result[2]
        assertEquals("7110", ytelsePerMaaned3.beloepBrutto?.beloep)
        assertEquals("2016-05-01", ytelsePerMaaned3.periode?.fom)
        assertEquals("2016-08-31", ytelsePerMaaned3.periode?.tom)
    }
}
