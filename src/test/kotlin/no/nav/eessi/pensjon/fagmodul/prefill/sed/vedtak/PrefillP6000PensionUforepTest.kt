package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjonMother.dummyEessiInfo
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.PensjonsinformasjonMother.pensjoninformasjonForSakstype
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.VedtakDataFromPENMother.fraFil
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.simpleFormat
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.trygdetid.V1Trygdetid
import no.nav.pensjon.v1.trygdetidliste.V1TrygdetidListe
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class PrefillP6000PensionUforepTest {

    @Test
    fun `forventet korrekt utfylling av Pensjon objekt på Uførepensjon`() {
        val prefill = initialPrefillDataModel("P6000", 66).apply {
            andreInstitusjon = dummyEessiInfo().asAndreinstitusjonerItem()
        }
        val dataFromPESYS = fraFil("P6000-UT-201.xml")

        val result = dataFromPESYS.prefill(prefill)

        assertNotNull(result.vedtak)
        assertNotNull(result.sak)
        assertNotNull(result.tilleggsinformasjon)

        val vedtak = result.vedtak?.get(0)
        assertEquals("2017-04-11" , vedtak?.virkningsdato, "vedtak.virkningsdato")
        assertEquals("02", vedtak?.type)
        assertEquals("02", vedtak?.basertPaa)
        assertEquals("03", vedtak?.resultat, "vedtak.resultat")
        assertEquals("2017-05-21", vedtak?.kjoeringsdato)
        assertEquals(null, vedtak?.artikkel, "4.1.5 vedtak.artikkel (må fylles ut manuelt nå)")

        assertEquals("01", vedtak?.grunnlag?.opptjening?.forsikredeAnnen)
        assertEquals("0", vedtak?.grunnlag?.framtidigtrygdetid)

        val beregning = vedtak?.beregning?.get(0)
        assertEquals("2017-05-01", beregning?.periode?.fom)
        assertEquals(null, beregning?.periode?.tom)
        assertEquals("NOK", beregning?.valuta)
        assertEquals("03", beregning?.utbetalingshyppighet)

        assertEquals("2482", beregning?.beloepBrutto?.beloep)
        assertEquals(null, beregning?.beloepBrutto?.ytelseskomponentGrunnpensjon)
        assertEquals(null, beregning?.beloepBrutto?.ytelseskomponentTilleggspensjon)

        assertEquals(null, vedtak?.ukjent?.beloepBrutto?.ytelseskomponentAnnen)

        val avslagBegrunnelse = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals(null, avslagBegrunnelse?.begrunnelse)

        assertEquals("six weeks from the date the decision is received", result.sak?.kravtype?.get(0)?.datoFrist)

        assertEquals("2017-05-21", result.tilleggsinformasjon?.dato)

        assertEquals("NO:noinst002", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsid)
        assertEquals("NOINST002, NO INST002, NO", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsnavn)
        assertEquals("Postboks 6600 Etterstad TEST", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsadresse)
        assertEquals("0607", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.postnummer)

    }

    @Test
    fun `forventet "08" ved  AvlsagsBegrunnelse på Uførepensjon ved TrygdleListeTom, HENSIKTSMESSIG_BEH`() {
        val dataFromPESYS = fraFil("P6000-UT-201.xml")

        val pendata = pensjoninformasjonForSakstype("UFOREP").apply {
            vedtak.isBoddArbeidetUtland = true
            trygdetidListe.trygdetidListe.clear()
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).vilkarsvurderingUforetrygd.hensiktsmessigBehandling = "HENSIKTSMESSIG_BEH"
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).vilkarsvurderingUforetrygd.alder = ""
        }

        assertEquals("08", dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata))
    }

    @Test
    fun `forventer "03" på AvlsagsBegrunnelse Gjenlevendepensjon, TrygdleListeTom`() {
        val dataFromPESYS = fraFil("P6000-UT-201.xml")

        val pendata = pensjoninformasjonForSakstype("UFOREP").apply {
            vedtak.isBoddArbeidetUtland = true
            trygdetidListe.trygdetidListe.clear()
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        }

        assertEquals("03", dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata))
    }

    @Test
    fun `forventer at ytelseprMaaned er siste i listen`() {
        val prefill = initialPrefillDataModel("P6000", 60).apply {
            vedtakId = "123456789"
        }
        val dataFromPESYS = fraFil("P6000-UT-220.xml")

        val pendata = dataFromPESYS.getPensjoninformasjonFraVedtak(prefill)

        val sisteprmnd = VedtakPensjonDataHelper.hentSisteYtelsePerMaaned(pendata)

        assertEquals("2017-05-01", sisteprmnd.fom.simpleFormat())
        assertEquals("7191", sisteprmnd.belop.toString())
        assertEquals(false, VedtakPensjonDataHelper.isMottarMinstePensjonsniva(pendata))
        assertEquals("7191", VedtakPensjonDataHelper.hentYtelseBelop(pendata))

        assertEquals(false, VedtakPensjonDataHelper.hentVurdertBeregningsmetodeNordisk(pendata))
        assertEquals("EOS", VedtakPensjonDataHelper.hentVinnendeBergeningsMetode(pendata))
    }

    @Test
    fun `forventet korrekt utregnet ytelsePrMnd på Uforep hvor UT_ORDINER`() {
        val prefill = initialPrefillDataModel("P6000", 60).apply {
            vedtakId = "123456789"
        }
        val dataFromPESYS = fraFil("P6000-UT-220.xml")

        val pendata = dataFromPESYS.getPensjoninformasjonFraVedtak(prefill)

        val result = dataFromPESYS.pensjonVedtak.createBeregningItemList(pendata)

        val json = mapAnyToJson(result, true)

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

    @Test
    fun `forventet createVedtakTypePensionWithRule verdi`() {
        val prefill = initialPrefillDataModel("P6000", 68)
        val dataFromPESYS = fraFil("P6000-UT-201.xml")
        val pendata = dataFromPESYS.getPensjoninformasjonFraVedtak(prefill)

        assertEquals("02", dataFromPESYS.pensjonVedtak.createVedtakTypePensionWithRule(pendata))
    }

    @Test(expected = IllegalStateException::class)
    fun `preutfylling P6000 feiler ved mangler av vedtakId`() {
        val dataFromPESYS = fraFil("P6000-UT-201.xml")

        val prefill = initialPrefillDataModel("P6000", 68).apply {
            vedtakId = ""
        }

        dataFromPESYS.prefill(prefill)
    }

    @Test(expected = java.lang.IllegalStateException::class)
    fun `feiler ved boddArbeidetUtland ikke sann`() {
        val prefill = initialPrefillDataModel("P6000", personAge = 66)

        val resdata = fraFil("P6000-AP-101.xml")

        resdata.prefill(prefill)
    }

    @Test
    fun `forventer "07" på AvlsagsBegrunnelse IKKE_MOTTATT_DOK`() {
        val dataFromPESYS = fraFil("P6000-UT-201.xml")

        val pendata = pensjoninformasjonForSakstype("ALDER").apply {
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "IKKE_MOTTATT_DOK"
        }

        assertEquals("07", dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata))
    }

}
