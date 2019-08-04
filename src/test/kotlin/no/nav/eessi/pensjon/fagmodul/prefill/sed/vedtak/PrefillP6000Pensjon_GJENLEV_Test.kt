package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjonMother.standardEessiInfo
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjonsinformasjonServiceMother.fraFil
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PrefillP6000Pensjon_GJENLEV_Test {

    @Test
    fun `forventet korrekt utfylling av Pensjon objekt på Gjenlevendepensjon`() {
        val dataFromPESYS = PensjonsinformasjonHjelper(fraFil("P6000-GP-401.xml"))

        val result = PrefillP6000Pensjon.createPensjon(dataFromPESYS, "12312312", standardEessiInfo().asAndreinstitusjonerItem())

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
    fun `forventet korrekt utfylt P6000 gjenlevende ikke bosat utland (avdød bodd i utland)`() {
        val dataFromPESYS = PensjonsinformasjonHjelper(fraFil("P6000-GP-IkkeUtland.xml"))

        val result = PrefillP6000Pensjon.createPensjon(dataFromPESYS, "12312312", standardEessiInfo().asAndreinstitusjonerItem())

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
        val dataFromPESYS = PensjonsinformasjonHjelper(fraFil("P6000-GP-401.xml"))

        PrefillP6000Pensjon.createPensjon(dataFromPESYS, "", null)
    }
}
