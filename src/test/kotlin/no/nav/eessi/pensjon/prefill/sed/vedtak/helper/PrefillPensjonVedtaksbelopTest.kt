package no.nav.eessi.pensjon.prefill.sed.vedtak.helper

import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonVedtaksbelop.createBeregningItemList
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjonsinformasjonClientMother
import no.nav.eessi.pensjon.utils.mapAnyToJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrefillPensjonVedtaksbelopTest {

    @Test
    fun `forventet korrekt utregnet ytelsePrMnd på Uforep hvor UT_ORDINER`() {
        val dataFromPESYS = PensjonsinformasjonService(PensjonsinformasjonClientMother.fraFil("P6000-UT-220.xml"), "q")
        val pendata = dataFromPESYS.hentMedVedtak("someVedtakId")

        val result = createBeregningItemList(pendata)

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
