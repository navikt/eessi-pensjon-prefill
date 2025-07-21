package no.nav.eessi.pensjon.prefill.sed.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.BasertPaa
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.EessiInformasjonMother.standardEessiInfo
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.EtterlatteService
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillP6000Pensjon_BARNEP_foreldrelos_Test {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(12)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(40)

    private lateinit var prefillNav: PrefillPDLNav
    private lateinit var prefillData: PrefillDataModel
    private lateinit var eessiInformasjon: EessiInformasjon
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var etterlatteService: EtterlatteService
    private lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var personDataCollection: PersonDataCollection


    @BeforeEach
    fun setup() {
        etterlatteService = mockk()
        personDataCollection = PersonPDLMock.createAvdodFamilie(personFnr, avdodPersonFnr)

        prefillNav = PrefillPDLNav(
            prefillAdresse = mockk<PrefillPDLAdresse> {
                every { hentLandkode(any()) } returns "NO"
                every { createPersonAdresse(any()) } returns mockk()
            },
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO"
        )

        eessiInformasjon = standardEessiInfo()

    }

    @Test
    fun `forventet korrekt utfylling av Pensjon objekt på Gjenlevendepensjon`() {
        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("/pensjonsinformasjon/vedtak/P6000-BARNEP-GJENLEV.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312", avdod = PersonInfo(avdodPersonFnr, "112233445566"))
        prefillSEDService = PrefillSEDService(eessiInformasjon, prefillNav)
        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)


        val p6000 = prefillSEDService.prefill(prefillData, personDataCollection,pensjonCollection) as P6000
        val p6000Pensjon = p6000.pensjon!!

        assertNotNull(p6000Pensjon.vedtak)
        assertNotNull(p6000Pensjon.sak)
        assertNotNull(p6000Pensjon.tilleggsinformasjon)

        val avdod = p6000.nav?.bruker?.person
        val gjenlev = p6000.pensjon?.gjenlevende!!

        assertEquals("BAMSE LUR", avdod?.fornavn)
        assertEquals("MOMBALO", avdod?.etternavn)

        assertEquals("BAMSE ULUR", gjenlev.person?.fornavn)
        assertEquals("DOLLY", gjenlev.person?.etternavn)

        val vedtak = p6000Pensjon.vedtak?.get(0)
        assertEquals("2020-08-01", vedtak?.virkningsdato, "vedtak.virkningsdato")
        assertEquals("03", vedtak?.type, "vedtak.type")
        assertEquals(BasertPaa.annet, vedtak?.basertPaa, "vedtak.basertPaa")
        assertEquals("01", vedtak?.resultat, "vedtak.resultat")

        assertEquals("03", vedtak?.grunnlag?.opptjening?.forsikredeAnnen)
        assertEquals("1", vedtak?.grunnlag?.framtidigtrygdetid)

        val beregning = vedtak?.beregning?.get(0)
        assertEquals("2020-08-01", beregning?.periode?.fom)
        assertEquals(null, beregning?.periode?.tom)
        assertEquals("NOK", beregning?.valuta)
        assertEquals("maaned_12_per_aar", beregning?.utbetalingshyppighet)

        assertEquals("16644", beregning?.beloepBrutto?.beloep)
        assertEquals("11246", beregning?.beloepBrutto?.ytelseskomponentGrunnpensjon)
        assertEquals("5398", beregning?.beloepBrutto?.ytelseskomponentTilleggspensjon)

        assertEquals(null, vedtak?.ukjent?.beloepBrutto?.ytelseskomponentAnnen)

        val avslagBegrunnelse = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals(null, avslagBegrunnelse?.begrunnelse)

        assertEquals("six weeks from the date the decision is received", p6000Pensjon.sak?.kravtype?.get(0)?.datoFrist)

        assertEquals("2020-08-21", p6000Pensjon.tilleggsinformasjon?.dato)
    }

    @Test
    fun `forventet korrekt utfylling av Pensjon objekt på Gjenlevendepensjon med AP_GJT_KAP19`() {
        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("/pensjonsinformasjon/vedtak/P6000-BARNEP-GJENLEV-Med-GJT_KAP19.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312", avdod = PersonInfo(avdodPersonFnr, "112233445566"))
        prefillSEDService = PrefillSEDService(eessiInformasjon, prefillNav)
        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)


        val p6000 = prefillSEDService.prefill(prefillData, personDataCollection,pensjonCollection) as P6000
        val p6000Pensjon = p6000.pensjon!!

        val vedtak = p6000Pensjon.vedtak?.get(0)
        val beregning = vedtak?.beregning?.get(0)
        assertEquals("24921", beregning?.beloepBrutto?.beloep)
        assertEquals("15215", beregning?.beloepBrutto?.ytelseskomponentGrunnpensjon)
        assertEquals("9706", beregning?.beloepBrutto?.ytelseskomponentTilleggspensjon)

//        assertEquals(null, vedtak?.ukjent?.beloepBrutto?.ytelseskomponentAnnen)

    }

}
