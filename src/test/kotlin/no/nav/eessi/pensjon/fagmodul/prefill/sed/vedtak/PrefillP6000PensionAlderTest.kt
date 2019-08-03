package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.PrefillVedtakTestHelper.eessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.PrefillVedtakTestHelper.generateFakePensjoninformasjonForKSAK
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.VedtakDataFromPENMother.fraFil
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.trygdetid.V1Trygdetid
import no.nav.pensjon.v1.trygdetidliste.V1TrygdetidListe
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class PrefillP6000PensionAlderTest {

    @Test
    fun `forventet korrekt utfylling av Pensjon objekt på Alderpensjon`() {
        val prefill = initialPrefillDataModel("P6000", 68)
        prefill.andreInstitusjon = eessiInformasjon.asAndreinstitusjonerItem()

        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val result = dataFromPESYS.prefill(prefill)

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

        assertEquals("NO:noinst002", tillegg?.andreinstitusjoner?.get(0)?.institusjonsid)
        assertEquals("NOINST002, NO INST002, NO", tillegg?.andreinstitusjoner?.get(0)?.institusjonsnavn)
        assertEquals("Postboks 6600 Etterstad TEST", tillegg?.andreinstitusjoner?.get(0)?.institusjonsadresse)
        assertEquals("0607", tillegg?.andreinstitusjoner?.get(0)?.postnummer)

    }

    @Test
    fun `forventet createVedtakTypePensionWithRule verdi`() {
        val prefill = initialPrefillDataModel("P6000", 68)
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")
        val pendata = dataFromPESYS.getPensjoninformasjonFraVedtak(prefill)

        //dataFromPESYS1.getPensjoninformasjonFraVedtak("23123123")
        val result = dataFromPESYS.pensjonVedtak.createVedtakTypePensionWithRule(pendata)
        assertEquals("01", result)
    }


    @Test
    fun `forventer "01" på AvlsagsBegrunnelse for Alderpensjon,Gjenlevende TrygdleListeTom `() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val pendata = generateFakePensjoninformasjonForKSAK("ALDER")
        pendata.vedtak.isBoddArbeidetUtland = true
        pendata.trygdetidListe.trygdetidListe.clear()
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"

        val result = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata)
        assertEquals("01", result)

        val pendata1 = generateFakePensjoninformasjonForKSAK("GJENLEV")
        pendata1.vedtak.isBoddArbeidetUtland = true
        pendata1.trygdetidListe.trygdetidListe.clear()
        pendata1.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        val result1 = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata1)
        assertEquals("01", result1)
    }

    @Test
    fun `forventer "03" på AvlsagsBegrunnelse for AlderPensjon TrygdleListeTom, LAVT_TIDLIG_UTTAK`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val pendata = generateFakePensjoninformasjonForKSAK("ALDER")
        pendata.vedtak.isBoddArbeidetUtland = true
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "LAVT_TIDLIG_UTTAK"
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"

        val result = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata)
        assertEquals("03", result)
    }

    @Test
    fun `forventer "13482" dager i sum summerTrygdeTid`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")
        val prefill = initialPrefillDataModel("P6000", 60)

        prefill.vedtakId = "121341234234"

        val pendata = dataFromPESYS.getPensjoninformasjonFraVedtak(prefill)

        val sumResult = dataFromPESYS.summerTrygdeTid(pendata.trygdetidListe)
        assertTrue( 13400 < sumResult)

    }

    @Test
    fun `forventer "06" på AvlsagsBegrunnelse AlderPensjon TrygdleListeTom, UNDER_62`() {

        val pendata = generateFakePensjoninformasjonForKSAK("ALDER")
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "UNDER_62"
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val result = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata)
        assertEquals("06", result)
    }


    @Test
    fun `sjekke enum correct value`() {
        val sakType = VedtakPensjonData.KSAK.valueOf("ALDER")
        assertEquals(sakType, VedtakPensjonData.KSAK.ALDER)

    }

    @Test(expected = IllegalStateException::class)
    fun `preutfylling P6000 feiler ved mangler av vedtakId`() {
        val prefill = initialPrefillDataModel("P6000", 68)
        prefill.vedtakId = ""
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")
        dataFromPESYS.prefill(prefill)
    }

    @Test
    fun `summerTrygdeTid forventet 10 dager, erTrygdeTid forventet til false`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val ttid1 = V1Trygdetid()
        ttid1.fom = PrefillVedtakTestHelper.convertToXMLcal(LocalDate.now().minusDays(50))
        ttid1.tom = PrefillVedtakTestHelper.convertToXMLcal(LocalDate.now().minusDays(40))


        val trygdetidListe = V1TrygdetidListe()
        trygdetidListe.trygdetidListe.add(ttid1)

        val result = dataFromPESYS.summerTrygdeTid(trygdetidListe)

        assertEquals(10, result)

        val pendata = Pensjonsinformasjon()
        pendata.trygdetidListe = trygdetidListe
        val bolresult = dataFromPESYS.erTrygdeTid(pendata)
        //bod i utland mindre totalt 10dager en mer en mindre en 30 og mindre en 360
        assertEquals(false, bolresult)
    }

    @Test
    fun `summerTrygdeTid forventet 70 dager, erTrygdeTid forventet til true`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val ttid1 = V1Trygdetid()
        ttid1.fom = PrefillVedtakTestHelper.convertToXMLcal(LocalDate.now().minusDays(170))
        ttid1.tom = PrefillVedtakTestHelper.convertToXMLcal(LocalDate.now().minusDays(100))

        val trygdetidListe = V1TrygdetidListe()
        trygdetidListe.trygdetidListe.add(ttid1)

        val result = dataFromPESYS.summerTrygdeTid(trygdetidListe)
        assertEquals(70, result)

        val pendata = Pensjonsinformasjon()
        pendata.trygdetidListe = trygdetidListe
        val bolresult = dataFromPESYS.erTrygdeTid(pendata)
        //bod i utland mindre en mer en 30 mindre en 360?
        assertEquals(true, bolresult)
    }

    @Test
    fun `summerTrygdeTid forventet 15 dager, erTrygdeTid forventet til false`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val trygdetidListe = PrefillVedtakTestHelper.createTrygdelisteTid()

        val result = dataFromPESYS.summerTrygdeTid(trygdetidListe)

        assertEquals(15, result)

        val pendata = Pensjonsinformasjon()
        pendata.trygdetidListe = trygdetidListe

        val bolresult = dataFromPESYS.erTrygdeTid(pendata)
        //bod for lite i utland mindre en 30 dager?
        assertEquals(false, bolresult)
    }

    @Test
    fun `summerTrygdeTid forventet 500 dager, erTrygdeTid forventet til false`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val ttid1 = V1Trygdetid()
        ttid1.fom = PrefillVedtakTestHelper.convertToXMLcal(LocalDate.now().minusDays(700))
        ttid1.tom = PrefillVedtakTestHelper.convertToXMLcal(LocalDate.now().minusDays(200))

        val trygdetidListe = V1TrygdetidListe()
        trygdetidListe.trygdetidListe.add(ttid1)

        val result = dataFromPESYS.summerTrygdeTid(trygdetidListe)

        assertEquals(500, result)

        val pendata = Pensjonsinformasjon()
        pendata.trygdetidListe = trygdetidListe

        val bolresult = dataFromPESYS.erTrygdeTid(pendata)
        //bod mye i utland mer en 360d.
        assertEquals(false, bolresult)
    }

    @Test
    fun `summerTrygdeTid forventet 0`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val fom = LocalDate.now().minusDays(0)
        val tom = LocalDate.now().plusDays(0)
        val trygdetidListe = V1TrygdetidListe()
        val ttid1 = V1Trygdetid()
        ttid1.fom = PrefillVedtakTestHelper.convertToXMLcal(fom)
        ttid1.tom = PrefillVedtakTestHelper.convertToXMLcal(tom)
        trygdetidListe.trygdetidListe.add(ttid1)
        val result = dataFromPESYS.summerTrygdeTid(trygdetidListe)
        assertEquals(0, result)
    }

    @Test(expected = java.lang.IllegalStateException::class)
    fun `feiler ved boddArbeidetUtland ikke sann`() {
        val prefill = initialPrefillDataModel("P6000", 66)
        val resdata = fraFil("P6000-AP-101.xml")
        resdata.prefill(prefill)
    }

    @Test
    fun `forventer "07" på AvlsagsBegrunnelse IKKE_MOTTATT_DOK`() {
        val dataFromPESYS = fraFil("P6000-APUtland-301.xml")

        val pendata = generateFakePensjoninformasjonForKSAK("ALDER")
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "IKKE_MOTTATT_DOK"
        val result = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata)
        assertEquals("07", result)
    }
}
