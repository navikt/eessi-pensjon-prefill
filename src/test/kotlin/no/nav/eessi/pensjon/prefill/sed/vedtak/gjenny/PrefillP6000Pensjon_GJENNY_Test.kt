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
                            ),
                            Utbetaling(
                                fraOgMed = LocalDate.parse("2018-11-01"), tilOgMed = LocalDate.parse("2030-11-01"),
                                beloep = "3432"
                            )
                        )
                    )
                )
            )
        )

        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("/pensjonsinformasjon/vedtak/P6000-GP-401.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312", avdod = PersonInfo(avdodPersonFnr, "1234567891234"), gjennySak = "OMSST")
        prefillSEDService = PrefillSEDService(standardEessiInfo(), prefillNav, etterlatteService)


        val p6000 = prefillSEDService.prefillGjenny(prefillData, personDataCollection) as P6000
        val p6000Pensjon = p6000.pensjon!!

        assertNotNull(p6000Pensjon.vedtak)
        assertNotNull(p6000Pensjon.tilleggsinformasjon)


        val krav = p6000Pensjon.sak?.kravtype?.get(0)
        assertEquals("01", krav?.krav)
        assertEquals("six weeks from the date the decision is received", krav?.datoFrist)

        val avdod = p6000.nav?.bruker?.person
        val gjenlev = p6000.pensjon?.gjenlevende!!

        assertEquals("THOR-DOPAPIR", avdod?.fornavn)
        assertEquals("RAGNAROK", avdod?.etternavn)

        assertEquals("ODIN ETTØYE", gjenlev.person?.fornavn)
        assertEquals("BALDER", gjenlev.person?.etternavn)


        val vedtak = p6000Pensjon.vedtak?.get(0)
        assertEquals("2018-05-01", vedtak?.virkningsdato, "vedtak.virkningsdato")
        assertEquals("01", vedtak?.type, "vedtak.type")

        val beregningDel1 = vedtak?.beregning?.get(0)
        assertEquals("NOK", beregningDel1?.valuta)
        assertEquals("2018-05-01", beregningDel1?.periode?.fom)
        assertEquals("2030-05-01", beregningDel1?.periode?.tom)
        assertEquals("5248", beregningDel1?.beloepBrutto?.beloep)
        assertEquals("maaned_12_per_aar", beregningDel1?.utbetalingshyppighet)

        val beregningDel2 = vedtak?.beregning?.get(1)
        assertEquals("NOK", beregningDel2?.valuta)
        assertEquals("2018-11-01", beregningDel2?.periode?.fom)
        assertEquals("2030-11-01", beregningDel2?.periode?.tom)
        assertEquals("3432", beregningDel2?.beloepBrutto?.beloep)
        assertEquals("maaned_12_per_aar", beregningDel2?.utbetalingshyppighet)

        val avslagBegrunnelse = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals(null, avslagBegrunnelse?.begrunnelse)
        assertEquals("six weeks from the date the decision is received", krav?.datoFrist)
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
