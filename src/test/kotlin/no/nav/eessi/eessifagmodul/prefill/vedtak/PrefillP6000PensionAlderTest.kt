package no.nav.eessi.eessifagmodul.prefill.vedtak

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class PrefillP6000PensionAlderTest: AbstractPensionDataFromPESYSTests("P6000-APUtland-301.xml") {


    @Test
    fun `forventet korrekt utfylling av Pensjon objekt på Alderpensjon`() {
        prefill = generatePrefillData(68, "P6000")

        //val dataFromPESYS1 = mockPrefillP6000PensionDataFromPESYS("P6000-APUtland-301.xml")
        //val result = dataFromPESYS1.prefill(prefill)

        val result = dataFromPESYS1.prefill(prefill)

        debugPrintFinalResult(result)

        val vedtaklst = result.vedtak
        val sak = result.sak
        val tillegg = result.tilleggsinformasjon
        assertNotNull(vedtaklst)
        assertNotNull(sak)
        assertNotNull(tillegg)

        val vedtak = vedtaklst?.get(0)
        assertEquals("2017-05-01" , vedtak?.virkningsdato, "4.1.6  pensjon.vedtak[x].virkningsdato")
        assertEquals("01", vedtak?.type, "4.1.1 vedtak.type")
        assertEquals("02", vedtak?.basertPaa, "4.1.2 vedtak.basertPaa")
        assertEquals("01", vedtak?.resultat, "4.1.4 vedtak.resultat ")
        assertEquals("2017-05-21", vedtak?.kjoeringsdato, "4.1.8 vedtak.kjoeringsdato")
        assertEquals(null, vedtak?.artikkel, "4.1.5 vedtak.artikkel (må fylles ut manuelt nå)")

        assertEquals("01", vedtak?.grunnlag?.opptjening?.forsikredeAnnen, "4.1.10 vedtak?.grunnlag?.opptjening?.forsikredeAnnen")
        assertEquals("0", vedtak?.grunnlag?.framtidigtrygdetid, "4.1.10 vedtak?.grunnlag?.framtidigtrygdetid")

        val bergen = vedtak?.beregning?.get(0)
        assertEquals("2017-05-01", bergen?.periode?.fom)
        assertEquals(null, bergen?.periode?.tom)
        assertEquals("NOK", bergen?.valuta)
        assertEquals("2017-05-01", bergen?.periode?.fom)
        assertEquals("03", bergen?.utbetalingshyppighet)

        assertEquals("11831", bergen?.beloepBrutto?.beloep)
        assertEquals("2719", bergen?.beloepBrutto?.ytelseskomponentGrunnpensjon)
        assertEquals("8996", bergen?.beloepBrutto?.ytelseskomponentTilleggspensjon)

        assertEquals("116", vedtak?.ukjent?.beloepBrutto?.ytelseskomponentAnnen)

        val avslagbrg = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals(null, avslagbrg?.begrunnelse, "4.1.13.1 vedtak?.avslagbegrunnelse?")

        val dataof = sak?.kravtype?.get(0)?.datoFrist
        assertEquals("six weeks from the date the decision is received", dataof)

        assertEquals("2017-05-21", tillegg?.dato)
        assertEquals(null, tillegg?.andreinstitusjoner?.get(0)?.institusjonsid)

    }

    @Test
    fun `forventet createVedtakTypePensionWithRule verdi`() {
        prefill = generatePrefillData(68, "P6000")
        //dataFromPESYS1.getPensjoninformasjonFraVedtak("23123123")
        val result = dataFromPESYS1.pensjonVedtak.createVedtakTypePensionWithRule(pendata)
        assertEquals("01", result)
    }


    @Test
    fun `forventer "01" på AvlsagsBegrunnelse for Alderpensjon,Gjenlevende TrygdleListeTom `() {

        val pendata = generateFakePensjoninformasjonForALDER()
        pendata.vedtak.isBoddArbeidetUtland = true
        pendata.trygdetidListe.trygdetidListe.clear()
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"

        val result = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata)
        assertEquals("01", result)

        val pendata1 = generateFakePensjoninformasjonForGJENLEV()
        pendata1.vedtak.isBoddArbeidetUtland = true
        pendata1.trygdetidListe.trygdetidListe.clear()
        pendata1.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        val result1 = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata1)
        assertEquals("01", result1)
    }

    @Test
    fun `forventer "03" på AvlsagsBegrunnelse for AlderPensjon TrygdleListeTom, LAVT_TIDLIG_UTTAK`() {
        val pendata = generateFakePensjoninformasjonForALDER()
        pendata.vedtak.isBoddArbeidetUtland = true
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "LAVT_TIDLIG_UTTAK"
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"

        val result = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata)
        assertEquals("03", result)
    }

    @Test
    fun `forventer "13482" dager i sum summerTrygdeTid`() {
        val dataFromPESYS1 = mockPrefillP6000PensionDataFromPESYS("P6000-APUtland-301.xml")
        val pendata = dataFromPESYS1.getPensjoninformasjonFraVedtak("342342342234")

        val sumResult = dataFromPESYS1.summerTrygdeTid(pendata.trygdetidListe)
        assertTrue( 13400 < sumResult)

    }

    @Test
    fun `forventer "06" på AvlsagsBegrunnelse AlderPensjon TrygdleListeTom, UNDER_62`() {

        val pendata = generateFakePensjoninformasjonForALDER()
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "UNDER_62"
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"

        val result = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata)
        assertEquals("06", result)
    }


    @Test
    fun `sjekke enum correct value`() {

        val sakType = PensjonData.KSAK.valueOf("ALDER")

        assertEquals(sakType, PensjonData.KSAK.ALDER)

    }

}