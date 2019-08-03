package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjonMother.dummyEessiInfo
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.PensjonsinformasjonMother.pensjoninformasjonForSakstype
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.VedtakDataFromPENMother.fraFil
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PrefillP6000PensionGjenlevTest {

    @Test
    fun `forventet korrekt utfylling av Pensjon objekt på Gjenlevendepensjon`() {
        val prefill = initialPrefillDataModel("vedtak", 66).apply {
            andreInstitusjon = dummyEessiInfo().asAndreinstitusjonerItem()
        }

        val dataFromPESYS = fraFil("P6000-GP-401.xml")

        val result = dataFromPESYS.prefill(prefill)

        //ekstra for å sjekke om Gjenlevepensjon finnes.
        val pendata = dataFromPESYS.getPensjoninformasjonFraVedtak(prefill.vedtakId)
        assertEquals("GJENLEV", pendata.sakAlder.sakType)
        assertEquals("12345678901", pendata.person.pid)

        assertNotNull(result.vedtak)
        assertNotNull(result.sak)
        assertNotNull(result.tilleggsinformasjon)

        val vedtak = result.vedtak?.get(0)
        assertEquals("2018-05-01" , vedtak?.virkningsdato, "vedtak.virkningsdato")
        assertEquals("03", vedtak?.type, "vedtak.type")
        assertEquals("02", vedtak?.basertPaa, "vedtak.basertPaa")
        assertEquals("03", vedtak?.resultat, "vedtak.resultat")
        assertEquals("2018-05-26", vedtak?.kjoeringsdato)
        assertEquals(null, vedtak?.artikkel, "4.1.5 vedtak.artikkel (må fylles ut manuelt nå)")

        assertEquals("03", vedtak?.grunnlag?.opptjening?.forsikredeAnnen)
        assertEquals("1", vedtak?.grunnlag?.framtidigtrygdetid)

        val beregning = vedtak?.beregning?.get(0)
        assertEquals("2018-05-01", beregning?.periode?.fom)
        assertEquals(null, beregning?.periode?.tom)
        assertEquals("NOK", beregning?.valuta)
        assertEquals("03", beregning?.utbetalingshyppighet)

        assertEquals("5248", beregning?.beloepBrutto?.beloep)
        assertEquals("3519", beregning?.beloepBrutto?.ytelseskomponentGrunnpensjon)
        assertEquals("1729", beregning?.beloepBrutto?.ytelseskomponentTilleggspensjon)

        assertEquals(null, vedtak?.ukjent?.beloepBrutto?.ytelseskomponentAnnen)

        val avslagBegrunnelse = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals(null, avslagBegrunnelse?.begrunnelse)

        assertEquals("six weeks from the date the decision is received", result.sak?.kravtype?.get(0)?.datoFrist)

        assertEquals("2018-05-26", result.tilleggsinformasjon?.dato)
    }

    @Test
    fun `forventer "01" på AvlsagsBegrunnelse Gjenlevendepensjon, TrygdleListeTom`() {
        val dataFromPESYS = fraFil("P6000-GP-401.xml")

        val pendata = pensjoninformasjonForSakstype("GJENLEV").apply {
            vedtak.isBoddArbeidetUtland = true
            trygdetidListe.trygdetidListe.clear()
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        }

        assertEquals("01", dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata))
    }


    @Test
    fun `forventet createVedtakTypePensionWithRule verdi`() {
        val prefill = initialPrefillDataModel("P6000", 68)
        val dataFromPESYS = fraFil("P6000-GP-401.xml")
        val pendata = dataFromPESYS.getPensjoninformasjonFraVedtak(prefill.vedtakId)

        assertEquals("03", dataFromPESYS.pensjonVedtak.createVedtakTypePensionWithRule(pendata))
    }

    @Test
    fun `forventet korrekt utfylt P6000 gjenlevende ikke bosat utland (avdød bodd i utland)`() {
        val prefill = initialPrefillDataModel("P6000", 66).apply {
            andreInstitusjon = dummyEessiInfo().asAndreinstitusjonerItem()
        }

        val dataFromPESYS = fraFil("P6000-GP-IkkeUtland.xml")

        val result = dataFromPESYS.prefill(prefill)

        assertNotNull(result.vedtak)
        assertNotNull(result.sak)
        assertNotNull(result.tilleggsinformasjon)

        val vedtak = result.vedtak?.get(0)
        assertEquals("2018-05-01", vedtak?.virkningsdato, "vedtak.virkningsdato")
        assertEquals("03", vedtak?.type, "vedtak.type")
        assertEquals("02", vedtak?.basertPaa, "vedtak.basertPaa")
        assertEquals("03", vedtak?.resultat, "vedtak.resultat")
        assertEquals("2018-05-26", vedtak?.kjoeringsdato)
        assertEquals(null, vedtak?.artikkel, "4.1.5 vedtak.artikkel (må fylles ut manuelt nå)")

        assertEquals("03", vedtak?.grunnlag?.opptjening?.forsikredeAnnen)
        assertEquals("1", vedtak?.grunnlag?.framtidigtrygdetid)

        val beregning = vedtak?.beregning?.get(0)
        assertEquals("2018-05-01", beregning?.periode?.fom)
        assertEquals(null, beregning?.periode?.tom)
        assertEquals("NOK", beregning?.valuta)
        assertEquals("03", beregning?.utbetalingshyppighet)

        assertEquals("6766", beregning?.beloepBrutto?.beloep)
        assertEquals("4319", beregning?.beloepBrutto?.ytelseskomponentGrunnpensjon)
        assertEquals("2447", beregning?.beloepBrutto?.ytelseskomponentTilleggspensjon)

        assertEquals(null, vedtak?.ukjent?.beloepBrutto?.ytelseskomponentAnnen)

        val avslagBegrunnelse = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals(null, avslagBegrunnelse?.begrunnelse)

        assertEquals("six weeks from the date the decision is received", result.sak?.kravtype?.get(0)?.datoFrist)

        assertEquals("2018-05-26", result.tilleggsinformasjon?.dato)

        assertEquals("NO:noinst002", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsid)
        assertEquals("NOINST002, NO INST002, NO", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsnavn)
        assertEquals("Postboks 6600 Etterstad TEST", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsadresse)
        assertEquals("0607", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.postnummer)
    }

    @Test(expected = IllegalStateException::class)
    fun `preutfylling P6000 feiler ved mangler av vedtakId`() {
        val prefill = initialPrefillDataModel("P6000", 68).apply {
            vedtakId = ""
        }
        val dataFromPESYS = fraFil("P6000-GP-401.xml")

        dataFromPESYS.prefill(prefill)
    }

    @Test(expected = java.lang.IllegalStateException::class)
    fun `feiler ved boddArbeidetUtland ikke sann`() {
        val prefill = initialPrefillDataModel("P6000", 66)

        val resdata = fraFil("P6000-AP-101.xml")

        resdata.prefill(prefill)
    }

    @Test
    fun `forventer "07" på AvlsagsBegrunnelse IKKE_MOTTATT_DOK`() {
        val dataFromPESYS = fraFil("P6000-GP-401.xml")

        val pendata = pensjoninformasjonForSakstype("ALDER").apply {
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "IKKE_MOTTATT_DOK"
        }

        assertEquals("07", dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata))
    }
}
