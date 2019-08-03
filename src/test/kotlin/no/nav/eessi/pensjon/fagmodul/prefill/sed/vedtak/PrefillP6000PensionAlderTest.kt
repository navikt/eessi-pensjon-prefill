package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjonMother.dummyEessiInfo
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.PensjonsinformasjonMother.pensjoninformasjonForSakstype
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.VedtakDataFromPENMother.fraFil
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
class PrefillP6000PensionAlderTest {

    @Test
    fun `forventet korrekt utfylling av Pensjon objekt på Alderpensjon`() {
        val prefill = initialPrefillDataModel("P6000", 68).apply {
            andreInstitusjon = dummyEessiInfo().asAndreinstitusjonerItem()
        }

        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val result = dataFromPESYS.prefill(prefill)

        assertNotNull(result.vedtak)
        assertNotNull(result.sak)
        assertNotNull(result.tilleggsinformasjon)

        val vedtak = result.vedtak?.get(0)
        assertEquals("2017-05-01" , vedtak?.virkningsdato, "4.1.6  pensjon.vedtak[x].virkningsdato")
        assertEquals("01", vedtak?.type, "4.1.1 vedtak.type")
        assertEquals("02", vedtak?.basertPaa, "4.1.2 vedtak.basertPaa")
        assertEquals("01", vedtak?.resultat, "4.1.4 vedtak.resultat ")
        assertEquals("2017-05-21", vedtak?.kjoeringsdato, "4.1.8 vedtak.kjoeringsdato")
        assertEquals(null, vedtak?.artikkel, "4.1.5 vedtak.artikkel (må fylles ut manuelt nå)")

        assertEquals("01", vedtak?.grunnlag?.opptjening?.forsikredeAnnen, "4.1.10 vedtak?.grunnlag?.opptjening?.forsikredeAnnen")
        assertEquals("0", vedtak?.grunnlag?.framtidigtrygdetid, "4.1.10 vedtak?.grunnlag?.framtidigtrygdetid")

        val beregning = vedtak?.beregning?.get(0)
        assertEquals("2017-05-01", beregning?.periode?.fom)
        assertEquals(null, beregning?.periode?.tom)
        assertEquals("NOK", beregning?.valuta)
        assertEquals("2017-05-01", beregning?.periode?.fom)
        assertEquals("03", beregning?.utbetalingshyppighet)

        assertEquals("11831", beregning?.beloepBrutto?.beloep)
        assertEquals("2719", beregning?.beloepBrutto?.ytelseskomponentGrunnpensjon)
        assertEquals("8996", beregning?.beloepBrutto?.ytelseskomponentTilleggspensjon)

        assertEquals("116", vedtak?.ukjent?.beloepBrutto?.ytelseskomponentAnnen)

        val avslagBegrunnelse = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals(null, avslagBegrunnelse?.begrunnelse, "4.1.13.1 vedtak?.avslagbegrunnelse?")

        assertEquals("six weeks from the date the decision is received", result.sak?.kravtype?.get(0)?.datoFrist)

        assertEquals("2017-05-21", result.tilleggsinformasjon?.dato)

        assertEquals("NO:noinst002", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsid)
        assertEquals("NOINST002, NO INST002, NO", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsnavn)
        assertEquals("Postboks 6600 Etterstad TEST", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsadresse)
        assertEquals("0607", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.postnummer)

    }

    @Test
    fun `forventet createVedtakTypePensionWithRule verdi`() {
        val prefill = initialPrefillDataModel("P6000", 68)
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")
        val pendata = dataFromPESYS.getPensjoninformasjonFraVedtak(prefill)

        assertEquals("01", dataFromPESYS.pensjonVedtak.createVedtakTypePensionWithRule(pendata))
    }


    @Test
    fun `forventer "01" på AvlsagsBegrunnelse for Alderpensjon,Gjenlevende TrygdleListeTom `() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val pendata = pensjoninformasjonForSakstype("ALDER").apply {
            vedtak.isBoddArbeidetUtland = true
            trygdetidListe.trygdetidListe.clear()
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        }

        assertEquals("01", dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata))

        val pendata1 = pensjoninformasjonForSakstype("GJENLEV").apply {
            vedtak.isBoddArbeidetUtland = true
            trygdetidListe.trygdetidListe.clear()
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"

        }
        assertEquals("01", dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata1))
    }

    @Test
    fun `forventer "03" på AvlsagsBegrunnelse for AlderPensjon TrygdleListeTom, LAVT_TIDLIG_UTTAK`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val pendata = pensjoninformasjonForSakstype("ALDER").apply {
            vedtak.isBoddArbeidetUtland = true
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "LAVT_TIDLIG_UTTAK"
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        }

        assertEquals("03", dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata))
    }

    @Test
    fun `forventer "13482" dager i sum summerTrygdeTid`() {
        val prefill = initialPrefillDataModel("P6000", 60).apply {
            vedtakId = "121341234234"
        }

        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val pendata = dataFromPESYS.getPensjoninformasjonFraVedtak(prefill)

        assertTrue( 13400 < dataFromPESYS.summerTrygdeTid(pendata.trygdetidListe))

    }

    @Test
    fun `forventer "06" på AvlsagsBegrunnelse AlderPensjon TrygdleListeTom, UNDER_62`() {
        val pendata = pensjoninformasjonForSakstype("ALDER").apply {
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "UNDER_62"
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        }

        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        assertEquals("06", dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata))
    }


    @Test
    fun `sjekke enum correct value`() {
        assertEquals(VedtakPensjonData.KSAK.ALDER, VedtakPensjonData.KSAK.valueOf("ALDER"))
    }

    @Test(expected = IllegalStateException::class)
    fun `preutfylling P6000 feiler ved mangler av vedtakId`() {
        val prefill = initialPrefillDataModel("P6000", 68).apply {
            vedtakId = ""
        }

        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        dataFromPESYS.prefill(prefill)
    }

    @Test
    fun `summerTrygdeTid forventet 10 dager, erTrygdeTid forventet til false`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val trygdetidListen = V1TrygdetidListe().apply {
            trygdetidListe.add(V1Trygdetid().apply {
                fom = 50.daysAgo()
                tom = 40.daysAgo()
            })
        }

        assertEquals(10, dataFromPESYS.summerTrygdeTid(trygdetidListen))

        val pendata = Pensjonsinformasjon().apply {
            trygdetidListe = trygdetidListen
        }

        //bod i utland mindre totalt 10dager en mer en mindre en 30 og mindre en 360
        assertFalse(dataFromPESYS.erTrygdeTid(pendata))
    }

    @Test
    fun `summerTrygdeTid forventet 70 dager, erTrygdeTid forventet til true`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val trygdetidListen = V1TrygdetidListe().apply {
            trygdetidListe.add(V1Trygdetid().apply {
                fom = 170.daysAgo()
                tom = 100.daysAgo()
            })
        }

        assertEquals(70, dataFromPESYS.summerTrygdeTid(trygdetidListen))

        val pendata = Pensjonsinformasjon().apply {
            trygdetidListe = trygdetidListen
        }

        //bod i utland mindre en mer en 30 mindre en 360?
        assertTrue( dataFromPESYS.erTrygdeTid(pendata))
    }

    @Test
    fun `summerTrygdeTid forventet 15 dager, erTrygdeTid forventet til false`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val trygdetidListen = PensjonsinformasjonMother.trePerioderMed5DagerHver()

        assertEquals(15, dataFromPESYS.summerTrygdeTid(trygdetidListen))

        val pendata = Pensjonsinformasjon().apply {
            trygdetidListe = trygdetidListen
        }

        //bod for lite i utland mindre en 30 dager?
        assertFalse(dataFromPESYS.erTrygdeTid(pendata))
    }

    @Test
    fun `summerTrygdeTid forventet 500 dager, erTrygdeTid forventet til false`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val trygdetidListen = V1TrygdetidListe().apply {
            trygdetidListe.add(V1Trygdetid().apply {
                fom = 700.daysAgo()
                tom = 200.daysAgo()
            })
        }

        val result = dataFromPESYS.summerTrygdeTid(trygdetidListen)

        assertEquals(500, result)

        val pendata = Pensjonsinformasjon().apply {
            trygdetidListe = trygdetidListen
        }

        //bod mye i utland mer en 360d.
        assertFalse(dataFromPESYS.erTrygdeTid(pendata))
    }

    @Test
    fun `summerTrygdeTid forventet 0`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val trygdetidListen = V1TrygdetidListe().apply {
            trygdetidListe.add(V1Trygdetid().apply {
                fom = 0.daysAgo()
                tom = 0.daysAhead()
            })
        }

        assertEquals(0, dataFromPESYS.summerTrygdeTid(trygdetidListen))
    }

    @Test(expected = java.lang.IllegalStateException::class)
    fun `feiler ved boddArbeidetUtland ikke sann`() {
        val prefill = initialPrefillDataModel("P6000", 66)
        val vedtakDataFromPEN = fraFil("P6000-AP-101.xml")

        vedtakDataFromPEN.prefill(prefill)
    }

    @Test
    fun `forventer "07" på AvlsagsBegrunnelse IKKE_MOTTATT_DOK`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val pendata = pensjoninformasjonForSakstype("ALDER").apply {
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
            vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "IKKE_MOTTATT_DOK"
        }

        assertEquals("07", dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata))
    }
}
