package no.nav.eessi.pensjon.prefill.sed.vedtak.helper

import no.nav.eessi.pensjon.prefill.sed.vedtak.PensjonsinformasjonMother
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonVedtaksavslag.createAvlsagsBegrunnelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrefillPensjonVedtaksavslagTest {


    @Test
    fun `forventer 01 på AvlsagsBegrunnelse for Alderpensjon TrygdleListeTom`() {
        val pendata = PensjonsinformasjonMother.pensjoninformasjonForSakstype("ALDER").apply {
            vedtak.isBoddArbeidetUtland = true
            trygdetidListe.trygdetidListe.clear()
            vilkarsvurderingListe.vilkarsvurderingListe[0].resultatHovedytelse = "AVSL"
        }

        assertEquals("01", createAvlsagsBegrunnelse(pendata))
    }

    @Test
    fun `forventer 01 på AvlsagsBegrunnelse for Gjenlevende TrygdleListeTom`() {
        val pendata1 = PensjonsinformasjonMother.pensjoninformasjonForSakstype("GJENLEV").apply {
            vedtak.isBoddArbeidetUtland = true
            trygdetidListe.trygdetidListe.clear()
            vilkarsvurderingListe.vilkarsvurderingListe[0].resultatHovedytelse = "AVSL"

        }
        assertEquals("01", createAvlsagsBegrunnelse(pendata1))
    }

    @Test
    fun `forventer 03 på AvlsagsBegrunnelse for AlderPensjon TrygdleListeTom, LAVT_TIDLIG_UTTAK`() {
        val pendata = PensjonsinformasjonMother.pensjoninformasjonForSakstype("ALDER").apply {
            vedtak.isBoddArbeidetUtland = true
            vilkarsvurderingListe.vilkarsvurderingListe[0].avslagHovedytelse = "LAVT_TIDLIG_UTTAK"
            vilkarsvurderingListe.vilkarsvurderingListe[0].resultatHovedytelse = "AVSL"
        }

        assertEquals("03", createAvlsagsBegrunnelse(pendata))
    }

    @Test
    fun `forventer 06 på AvlsagsBegrunnelse AlderPensjon TrygdleListeTom, UNDER_62`() {
        val pendata = PensjonsinformasjonMother.pensjoninformasjonForSakstype("ALDER").apply {
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "UNDER_62"
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSL"
        }
        assertEquals("06", createAvlsagsBegrunnelse(pendata))
    }

    @Test
    fun `forventer 07 på AvlsagsBegrunnelse IKKE_MOTTATT_DOK`() {
        val pendata = PensjonsinformasjonMother.pensjoninformasjonForSakstype("ALDER").apply {
            vilkarsvurderingListe.vilkarsvurderingListe[0].resultatHovedytelse = "AVSL"
            vilkarsvurderingListe.vilkarsvurderingListe[0].avslagHovedytelse = "IKKE_MOTTATT_DOK"
        }

        assertEquals("07", createAvlsagsBegrunnelse(pendata))
    }

    @Test
    fun `forventer 01 på AvlsagsBegrunnelse Gjenlevendepensjon, TrygdleListeTom`() {
        val pendata = PensjonsinformasjonMother.pensjoninformasjonForSakstype("GJENLEV").apply {
            vedtak.isBoddArbeidetUtland = true
            trygdetidListe.trygdetidListe.clear()
            vilkarsvurderingListe.vilkarsvurderingListe[0].resultatHovedytelse = "AVSL"
        }

        assertEquals("01", createAvlsagsBegrunnelse(pendata))
    }

    @Test
    fun `forventer 03 på AvlsagsBegrunnelse Uførepensjon, TrygdleListeTom`() {
        val pendata = PensjonsinformasjonMother.pensjoninformasjonForSakstype("UFOREP").apply {
            vedtak.isBoddArbeidetUtland = true
            trygdetidListe.trygdetidListe.clear()
            vilkarsvurderingListe.vilkarsvurderingListe[0].resultatHovedytelse = "AVSL"
        }

        assertEquals("03", createAvlsagsBegrunnelse(pendata))
    }

    @Test
    fun `forventet 08 ved  AvlsagsBegrunnelse på Uførepensjon ved TrygdleListeTom, HENSIKTSMESSIG_BEH`() {
        val pendata = PensjonsinformasjonMother.pensjoninformasjonForSakstype("UFOREP").apply {
            vedtak.isBoddArbeidetUtland = true
            trygdetidListe.trygdetidListe.clear()
            vilkarsvurderingListe.vilkarsvurderingListe[0].vilkarsvurderingUforetrygd.hensiktsmessigBehandling = "HENSIKTSMESSIG_BEH"
            vilkarsvurderingListe.vilkarsvurderingListe[0].resultatHovedytelse = "AVSL"
            vilkarsvurderingListe.vilkarsvurderingListe[0].vilkarsvurderingUforetrygd.alder = ""
        }

        assertEquals("08", createAvlsagsBegrunnelse(pendata))
    }
}
