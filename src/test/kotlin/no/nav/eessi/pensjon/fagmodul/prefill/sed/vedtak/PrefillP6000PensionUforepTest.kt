package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.PrefillVedtakTestHelper.eessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.PrefillVedtakTestHelper.generateFakePensjoninformasjonForKSAK
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.VedtakDataFromPENMother.fraFil
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.simpleFormat
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.trygdetid.V1Trygdetid
import no.nav.pensjon.v1.trygdetidliste.V1TrygdetidListe
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PrefillP6000PensionUforepTest {

    @Test
    fun `forventet korrekt utfylling av Pensjon objekt på Uførepensjon`() {
        val prefill = initialPrefillDataModel("P6000", 66)
        prefill.andreInstitusjon = eessiInformasjon.asAndreinstitusjonerItem()

        val dataFromPESYS = fraFil("P6000-UT-201.xml")

        val result = dataFromPESYS.prefill(prefill)

        val vedtaklst = result.vedtak
        val sak = result.sak
        val tillegg = result.tilleggsinformasjon
        assertNotNull(vedtaklst)
        assertNotNull(sak)
        assertNotNull(tillegg)

        val vedtak = vedtaklst?.get(0)
        assertEquals("2017-04-11" , vedtak?.virkningsdato, "vedtak.virkningsdato")
        assertEquals("02", vedtak?.type)
        assertEquals("02", vedtak?.basertPaa)
        assertEquals("03", vedtak?.resultat, "vedtak.resultat")
        assertEquals("2017-05-21", vedtak?.kjoeringsdato)
        assertEquals(null, vedtak?.artikkel, "4.1.5 vedtak.artikkel (må fylles ut manuelt nå)")

        assertEquals("01", vedtak?.grunnlag?.opptjening?.forsikredeAnnen)
        assertEquals("0", vedtak?.grunnlag?.framtidigtrygdetid)

        val bergen = vedtak?.beregning?.get(0)
        assertEquals("2017-05-01", bergen?.periode?.fom)
        assertEquals(null, bergen?.periode?.tom)
        assertEquals("NOK", bergen?.valuta)
        assertEquals("03", bergen?.utbetalingshyppighet)

        assertEquals("2482", bergen?.beloepBrutto?.beloep)
        assertEquals(null, bergen?.beloepBrutto?.ytelseskomponentGrunnpensjon)
        assertEquals(null, bergen?.beloepBrutto?.ytelseskomponentTilleggspensjon)

        assertEquals(null, vedtak?.ukjent?.beloepBrutto?.ytelseskomponentAnnen)

        val avslagbrg = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals(null, avslagbrg?.begrunnelse)

        val dataof = sak?.kravtype?.get(0)?.datoFrist
        assertEquals("six weeks from the date the decision is received", dataof)

        assertEquals("2017-05-21", tillegg?.dato)

        assertEquals("NO:noinst002", tillegg?.andreinstitusjoner?.get(0)?.institusjonsid)
        assertEquals("NOINST002, NO INST002, NO", tillegg?.andreinstitusjoner?.get(0)?.institusjonsnavn)
        assertEquals("Postboks 6600 Etterstad TEST", tillegg?.andreinstitusjoner?.get(0)?.institusjonsadresse)
        assertEquals("0607", tillegg?.andreinstitusjoner?.get(0)?.postnummer)

    }

    @Test
    fun `forventet "08" ved  AvlsagsBegrunnelse på Uførepensjon ved TrygdleListeTom, HENSIKTSMESSIG_BEH`() {
        val dataFromPESYS = fraFil("P6000-UT-201.xml")
        val pendata = generateFakePensjoninformasjonForKSAK("UFOREP")
        pendata.vedtak.isBoddArbeidetUtland = true
        pendata.trygdetidListe.trygdetidListe.clear()
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).vilkarsvurderingUforetrygd.hensiktsmessigBehandling = "HENSIKTSMESSIG_BEH"
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).vilkarsvurderingUforetrygd.alder = ""
        val result = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata)
        assertEquals("08", result)
    }

    @Test
    fun `forventer "03" på AvlsagsBegrunnelse Gjenlevendepensjon, TrygdleListeTom`() {
        val dataFromPESYS = fraFil("P6000-UT-201.xml")
        val pendata = generateFakePensjoninformasjonForKSAK("UFOREP")
        pendata.vedtak.isBoddArbeidetUtland = true
        pendata.trygdetidListe.trygdetidListe.clear()
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        val result1 = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata)
        assertEquals("03", result1)
    }

    @Test
    fun `forventer at ytelseprMaaned er siste i listen`() {
        val dataFromPESYS = fraFil("P6000-UT-220.xml")
        val prefill = initialPrefillDataModel("P6000", 60)
        prefill.vedtakId = "123456789"
        val pendata = dataFromPESYS.getPensjoninformasjonFraVedtak(prefill)

        val sisteprmnd = dataFromPESYS.hentSisteYtelsePerMaaned(pendata)

        assertEquals("2017-05-01", sisteprmnd.fom.simpleFormat())
        assertEquals("7191", sisteprmnd.belop.toString())
        assertEquals(false, dataFromPESYS.isMottarMinstePensjonsniva(pendata))
        assertEquals("7191", dataFromPESYS.hentYtelseBelop(pendata))

        assertEquals(false, dataFromPESYS.hentVurdertBeregningsmetodeNordisk(pendata))
        assertEquals("EOS", dataFromPESYS.hentVinnendeBergeningsMetode(pendata))
    }

    @Test
    fun `forventet korrekt utregnet ytelsePrMnd på Uforep hvor UT_ORDINER`() {
        val dataFromPESYS = fraFil("P6000-UT-220.xml")

        val prefill = initialPrefillDataModel("P6000", 60)
        prefill.vedtakId = "123456789"
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

        val result = dataFromPESYS.pensjonVedtak.createVedtakTypePensionWithRule(pendata)
        assertEquals("02", result)
    }

    @Test(expected = IllegalStateException::class)
    fun `preutfylling P6000 feiler ved mangler av vedtakId`() {
        val dataFromPESYS = fraFil("P6000-UT-201.xml")

        val prefill = initialPrefillDataModel("P6000", 68)
        prefill.vedtakId = ""

        dataFromPESYS.prefill(prefill)
    }

    @Test
    fun `summerTrygdeTid forventet 10 dager, erTrygdeTid forventet til false`() {
        val dataFromPESYS = fraFil("P6000-UT-201.xml")

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
        val dataFromPESYS = fraFil("P6000-UT-201.xml")

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
        val dataFromPESYS = fraFil("P6000-UT-201.xml")

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
        val dataFromPESYS = fraFil("P6000-UT-201.xml")

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
        val dataFromPESYS = fraFil("P6000-UT-201.xml")

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
        val prefill = initialPrefillDataModel("P6000", personAge = 66)

        val resdata = fraFil("P6000-AP-101.xml")
        resdata.prefill(prefill)
    }

    @Test
    fun `forventer "07" på AvlsagsBegrunnelse IKKE_MOTTATT_DOK`() {
        val dataFromPESYS = fraFil("P6000-UT-201.xml")

        val pendata = generateFakePensjoninformasjonForKSAK("ALDER")
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).resultatHovedytelse = "AVSLAG"
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.get(0).avslagHovedytelse = "IKKE_MOTTATT_DOK"
        val result = dataFromPESYS.pensjonVedtak.createAvlsagsBegrunnelse(pendata)
        assertEquals("07", result)
    }

}
