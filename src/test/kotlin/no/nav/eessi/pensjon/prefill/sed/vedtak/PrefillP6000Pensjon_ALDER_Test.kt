package no.nav.eessi.pensjon.prefill.sed.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.BasertPaa
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.integrationtest.XmlToP6000Mapper
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.utils.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException

class PrefillP6000Pensjon_ALDER_Test {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(67)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var personDataCollection: PersonDataCollection
    private val pesysService: PesysService = mockk()

    @BeforeEach
    fun setup() {
        prefillSEDService = BasePrefillNav.createPrefillSEDService()
        personDataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)
    }

    @Test
    fun `forventet korrekt utfylling av Pensjon objekt på Alderpensjon`() {
        every { pesysService.hentP6000data(any()) } returns XmlToP6000Mapper.readP6000FromXml("/pensjonsinformasjon/vedtak/P6000-APUtland-301.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312")
        val innhentingService = InnhentingService(mockk(), pesysService = pesysService)
        val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        val p6000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null,) as P6000
        val p6000Pensjon = p6000.pensjon!!

        assertNotNull(p6000Pensjon.vedtak)
        assertNotNull(p6000Pensjon.sak)
        assertNotNull(p6000Pensjon.tilleggsinformasjon)

        assertEquals(1, p6000Pensjon.vedtak?.size, "4.1  pensjon.vedtak")

        val vedtak = p6000Pensjon.vedtak?.firstOrNull()
        assertEquals("2017-05-01", vedtak?.virkningsdato, "4.1.6  pensjon.vedtak[x].virkningsdato")
        assertEquals("01", vedtak?.type, "4.1.1 vedtak.type")
        assertEquals(BasertPaa.i_arbeid, vedtak?.basertPaa, "4.1.2 vedtak.basertPaa")

        assertEquals(null, vedtak?.basertPaaAnnen, "4.1.3.1 artikkel.basertPaaAnnen")
        assertEquals("01", vedtak?.resultat, "4.1.4 vedtak.resultat ")

        assertEquals("01", vedtak?.grunnlag?.opptjening?.forsikredeAnnen, "4.1.10 vedtak?.grunnlag?.opptjening?.forsikredeAnnen")
        assertEquals("0", vedtak?.grunnlag?.framtidigtrygdetid, "4.1.10 vedtak?.grunnlag?.framtidigtrygdetid")

        assertEquals(null, vedtak?.avslagbegrunnelse, "4.1.13.1 -- 4.1.13.2.1")

        println("vedtak?.beregning: ${vedtak?.beregning?.toJson()}")
        assertEquals(1, vedtak?.beregning?.size, "4.1.7 vedtak?.beregning")
        val beregning = vedtak?.beregning?.firstOrNull()

        assertEquals("2017-05-01", beregning?.periode?.fom)
        assertEquals(null, beregning?.periode?.tom)
        assertEquals("NOK", beregning?.valuta)
        assertEquals("2017-05-01", beregning?.periode?.fom)
        assertEquals("maaned_12_per_aar", beregning?.utbetalingshyppighet)
        assertEquals("11831", beregning?.beloepBrutto?.beloep)
        assertEquals("2719", beregning?.beloepBrutto?.ytelseskomponentGrunnpensjon)
        assertEquals("8996", beregning?.beloepBrutto?.ytelseskomponentTilleggspensjon)
        assertEquals("116", vedtak?.ukjent?.beloepBrutto?.ytelseskomponentAnnen)

        val avslagBegrunnelse = vedtak?.avslagbegrunnelse?.first()

        assertEquals(null, avslagBegrunnelse?.begrunnelse, "4.1.13.1 vedtak?.avslagbegrunnelse?")
        assertEquals("six weeks from the date the decision is received", p6000Pensjon.sak?.kravtype?.get(0)?.datoFrist)
        assertEquals("2017-05-21", p6000Pensjon.tilleggsinformasjon?.dato)

        assertEquals("NO:noinst002", p6000Pensjon.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsid)
        assertEquals("Postboks 6600 Etterstad TEST", p6000Pensjon.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsadresse)
        assertEquals("0607", p6000Pensjon.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.postnummer)
    }

    @Test
    fun `feiler ved boddArbeidetUtland ikke sann`() {
        every { pesysService.hentP6000data(any()) } returns XmlToP6000Mapper.readP6000FromXml("/pensjonsinformasjon/vedtak/P6000-AP-101.xml")

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312")
        val innhentingService = InnhentingService(mockk(), pesysService)
        val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        assertThrows<ResponseStatusException> {
            prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null,)
        }
    }

    @Test
    fun `henting av bruttobelop skal hente verdier fra garantipensjon, grunnpensjon, pensjontillegg, inntektspensjon, saertillegg `() {
        every { pesysService.hentP6000data(any()) } returns XmlToP6000Mapper.readP6000FromXml("/pensjonsinformasjon/vedtak/P6000-AP-GP-301.xml")

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312")
        val innhentingService = InnhentingService(mockk(), pesysService)
        val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        val p6000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null,) as P6000
        val p6000Pensjon = p6000.pensjon!!

        val beregning = p6000Pensjon.vedtak?.first { it.beregning != null }
        val bruttpGrunnPensjonsbelop = beregning?.beregning?.firstOrNull { it.beloepBrutto != null }

        assertNotNull(p6000Pensjon.vedtak)
        assertNotNull(p6000Pensjon.sak)
        assertNotNull(p6000Pensjon.tilleggsinformasjon)

        assertEquals(1, p6000Pensjon.vedtak?.size, "4.1  pensjon.vedtak")
//        Skal vise summen av GP + PT + GAP + GAT + ST skal i denne testen tilsvare 100 + 200 + 100 + 400 + 2719
        assertEquals("3519", bruttpGrunnPensjonsbelop?.beloepBrutto?.ytelseskomponentGrunnpensjon)

    }
}
