package no.nav.eessi.pensjon.prefill.sed.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
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
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException

class PrefillP6000Pensjon_UFORE_Test {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(67)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var prefillNav: PrefillPDLNav
    private lateinit var eessiInformasjon: EessiInformasjon
    private lateinit var personDataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        personDataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)

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
    fun `forventet korrekt utfylling av Pensjon objekt på Uførepensjon`() {

        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("/pensjonsinformasjon/vedtak/P6000-UT-201.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.SEDTYPE_P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312")
        prefillSEDService = PrefillSEDService(eessiInformasjon, prefillNav)
        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        val p6000 = prefillSEDService.prefill(prefillData, personDataCollection,pensjonCollection) as P6000
        val p6000Pensjon = p6000.pensjon!!

        assertNotNull(p6000Pensjon.vedtak)
        assertNotNull(p6000Pensjon.sak)
        assertNotNull(p6000Pensjon.tilleggsinformasjon)

        val vedtak = p6000Pensjon.vedtak?.get(0)
        assertEquals("2017-04-11", vedtak?.virkningsdato, "vedtak.virkningsdato")
        assertEquals("02", vedtak?.type)
        assertEquals("02", vedtak?.basertPaa)
        assertEquals("03", vedtak?.resultat, "vedtak.resultat")

        assertEquals("01", vedtak?.grunnlag?.opptjening?.forsikredeAnnen)
        assertEquals("1", vedtak?.grunnlag?.framtidigtrygdetid)

        val beregning = vedtak?.beregning?.get(0)
        assertEquals("2017-05-01", beregning?.periode?.fom)
        assertEquals(null, beregning?.periode?.tom)
        assertEquals("NOK", beregning?.valuta)
        assertEquals("maaned_12_per_aar", beregning?.utbetalingshyppighet)

        assertEquals("2482", beregning?.beloepBrutto?.beloep)
        assertEquals(null, beregning?.beloepBrutto?.ytelseskomponentGrunnpensjon)
        assertEquals(null, beregning?.beloepBrutto?.ytelseskomponentTilleggspensjon)

        assertEquals(null, vedtak?.ukjent?.beloepBrutto?.ytelseskomponentAnnen)

        val avslagBegrunnelse = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals(null, avslagBegrunnelse?.begrunnelse)

        assertEquals("six weeks from the date the decision is received", p6000Pensjon.sak?.kravtype?.get(0)?.datoFrist)

        assertEquals("2017-05-21", p6000Pensjon.tilleggsinformasjon?.dato)

        assertEquals("NO:noinst002", p6000Pensjon.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsid)
        assertEquals("Postboks 6600 Etterstad TEST", p6000Pensjon.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsadresse)
        assertEquals("0607", p6000Pensjon.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.postnummer)

    }

    @Test
    fun `preutfylling P6000 feiler ved mangler av vedtakId`() {
        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("/pensjonsinformasjon/vedtak/P6000-UT-201.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.SEDTYPE_P6000, personFnr, penSaksnummer = "22580170", vedtakId = "")
        prefillSEDService = PrefillSEDService(eessiInformasjon, prefillNav)
        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)

        assertThrows<ResponseStatusException> {
            innhentingService.hentPensjoninformasjonCollection(prefillData)
//            prefillSEDService.prefill(prefillData, personDataCollection,pensjonCollection)
        }
    }
}
