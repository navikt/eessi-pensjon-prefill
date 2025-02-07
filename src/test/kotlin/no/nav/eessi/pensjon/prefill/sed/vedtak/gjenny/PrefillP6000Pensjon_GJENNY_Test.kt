package no.nav.eessi.pensjon.prefill.sed.vedtak.gjenny

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.etterlatte.*
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PrefillP6000Pensjon_GJENNY_Test {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(57)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(63)

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var prefillNav: PrefillPDLNav
    private lateinit var vedtakInformasjonFraGjenny: EtterlatteResponse
    private lateinit var personDataCollection: PersonDataCollection
    private var etterlatteService: EtterlatteService = mockk()

    @BeforeEach
    fun setup() {
        val personDataCollectionFamilie = PersonPDLMock.createEnkelFamilie(personFnr, avdodPersonFnr)
        personDataCollection = PersonDataCollection(gjenlevendeEllerAvdod = personDataCollectionFamilie.ektefellePerson, forsikretPerson = personDataCollectionFamilie.forsikretPerson )

        prefillNav = PrefillPDLNav(
            prefillAdresse = mockk<PrefillPDLAdresse> {
                every { hentLandkode(any()) } returns "NO"
                every { createPersonAdresse(any()) } returns mockk()
            },
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO"
        )

    }

    @Test
    fun `forventet korrekt utfylling av Pensjon objekt på Gjenlevendepensjon`() {
        every { etterlatteService.hentGjennySak(any()) } returns Result.success(
            EtterlatteResponse(
                vedtak = listOf(
                    Vedtak(
                        sakId = 123456,
                        sakType = "GJENLEV",
                        virkningstidspunkt = LocalDate.parse("2018-05-01"),
                        type = "INNVILGELSE",
                        utbetaling = listOf(
                            Utbetaling(
                                fraOgMed = LocalDate.parse("2018-05-01"), tilOgMed = LocalDate.parse("2030-05-01"),
                                beloep = "5248"
                            )
                        )
                    )
                )
            )
        )

        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("/pensjonsinformasjon/vedtak/P6000-GP-401.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312", avdod = PersonInfo(avdodPersonFnr, "1234567891234"))
        prefillSEDService = PrefillSEDService(standardEessiInfo(), prefillNav, etterlatteService)


        val p6000 = prefillSEDService.prefillGjenny(prefillData, personDataCollection) as P6000
        val p6000Pensjon = p6000.pensjon!!

        assertNotNull(p6000Pensjon.vedtak)
        assertNotNull(p6000Pensjon.sak)
        assertNotNull(p6000Pensjon.tilleggsinformasjon)

        val avdod = p6000.nav?.bruker?.person
        val gjenlev = p6000.pensjon?.gjenlevende!!

        assertEquals("THOR-DOPAPIR", avdod?.fornavn)
        assertEquals("RAGNAROK", avdod?.etternavn)

        assertEquals("ODIN ETTØYE", gjenlev.person?.fornavn)
        assertEquals("BALDER", gjenlev.person?.etternavn)


        val vedtak = p6000Pensjon.vedtak?.get(0)
        assertEquals("2018-05-01", vedtak?.virkningsdato, "vedtak.virkningsdato")
        assertEquals("02", vedtak?.type, "vedtak.type")

//        assertEquals(BasertPaa.i_arbeid, vedtak?.basertPaa, "vedtak.basertPaa")
//        assertEquals("03", vedtak?.resultat, "vedtak.resultat")

//        assertEquals("03", vedtak?.grunnlag?.opptjening?.forsikredeAnnen)
//        assertEquals("1", vedtak?.grunnlag?.framtidigtrygdetid)

        val beregning = vedtak?.beregning?.get(0)
        assertEquals("2018-05-01", beregning?.periode?.fom)
        assertEquals("2030-05-01", beregning?.periode?.tom)
        assertEquals("NOK", beregning?.valuta)
//        assertEquals("maaned_12_per_aar", beregning?.utbetalingshyppighet)

        assertEquals("5248", beregning?.beloepBrutto?.beloep)
//        assertEquals("3519", beregning?.beloepBrutto?.ytelseskomponentGrunnpensjon)
//        assertEquals("1729", beregning?.beloepBrutto?.ytelseskomponentTilleggspensjon)
//        assertEquals(null, vedtak?.ukjent?.beloepBrutto?.ytelseskomponentAnnen)

        val avslagBegrunnelse = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals(null, avslagBegrunnelse?.begrunnelse)
        assertEquals("six weeks from the date the decision is received", p6000Pensjon.sak?.kravtype?.get(0)?.datoFrist)
        //TODO: Kan vi spørre om vi kan få dato da vedtaket ble fattet
//        assertEquals("2018-05-26", p6000Pensjon.tilleggsinformasjon?.dato)
    }

    fun standardEessiInfo() = EessiInformasjon(
        institutionid = "NO:noinst002",
        institutionnavn = "NOINST002, NO INST002, NO",
        institutionGate = "Postboks 6600 Etterstad TEST",
        institutionBy = "Oslo",
        institutionPostnr = "0607",
        institutionLand = "NO"
    )
}
