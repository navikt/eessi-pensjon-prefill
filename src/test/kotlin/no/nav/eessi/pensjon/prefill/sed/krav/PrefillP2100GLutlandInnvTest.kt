package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.models.pensjon.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.models.YtelseskomponentType
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.Ytelseskomponent
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.krav.PensjonsInformasjonHelper.readJsonResponse
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PrefillP2100GLutlandInnvTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(65)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(75)
    private val pesysSaksnummer = "22875355"
    private val pesysService : PesysService = mockk()

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var pensjonCollection: PensjonCollection
    private lateinit var persondataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        every { pesysService.hentP2100data(any(),any(),any()) } returns mockk(){
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiFellesDto.EessiSakType.ALDER,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.of(2019, 6, 1),
                        kravType = EessiFellesDto.EessiKravGjelder.F_BH_KUN_UTL,
                        virkningstidspunkt = LocalDate.of(2015, 11, 25),
                        kravAarsak = EessiFellesDto.EessiKravAarsak.NY_SOKNAD
                    )
                ),
                ytelsePerMaaned = listOf(
                    P2xxxMeldingOmPensjonDto.YtelsePerMaaned(
                        fom = LocalDate.of(2015, 11, 25),
                        belop = 123,
                        ytelseskomponentListe = listOf(
                            Ytelseskomponent(
                                YtelseskomponentType.GAP.name,
                                444
                            )
                        )
                    )
                ),
                forsteVirkningstidspunkt = LocalDate.of(2025, 12, 12),
                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
            )
            every { vedtak } returns P2xxxMeldingOmPensjonDto.Vedtak(boddArbeidetUtland = true)
        }
        persondataCollection = PersonPDLMock.createAvdodFamilie(personFnr, avdodPersonFnr)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(
                sedType = SedType.P2100,
                pinId = personFnr,
                penSaksnummer = pesysSaksnummer,
                avdod = PersonInfo(avdodPersonFnr, "112233445566")
        ).apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
            partSedAsJson["P4000"] = readJsonResponse("/json/nav/other/p4000_trygdetid_part.json")

        }
        val innhentingService = InnhentingService(mockk(), pesysService = pesysService)
        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        prefillSEDService = BasePrefillNav.createPrefillSEDService()

    }

    @Test
    fun `forventet korrekt utfylt P2100 uforepensjon med kap4 og 9`() {
        val p2100 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection, null,)

        val p2100gjenlev = SED(
                type = SedType.P2100,
                pensjon = p2100.pensjon,
                nav = Nav(krav = p2100.nav?.krav)
        )

        assertNotNull(p2100gjenlev.nav?.krav)
        assertEquals("2019-06-01", p2100gjenlev.nav?.krav?.dato)

    }

    @Test
    fun `forventet korrekt utfylt P2100 uforepensjon med mockdata fra testfiler`() {
        val p2100 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection, null,)

        assertEquals(null, p2100.nav?.barn)

        assertEquals("", p2100.nav?.bruker?.arbeidsforhold?.get(0)?.yrke)
        assertEquals("2018-11-11", p2100.nav?.bruker?.arbeidsforhold?.get(0)?.planlagtstartdato)
        assertEquals("2018-11-13", p2100.nav?.bruker?.arbeidsforhold?.get(0)?.planlagtpensjoneringsdato)
        assertEquals("07", p2100.nav?.bruker?.arbeidsforhold?.get(0)?.type)

        assertEquals("foo", p2100.nav?.bruker?.bank?.navn)
        assertEquals("bar", p2100.nav?.bruker?.bank?.konto?.sepa?.iban)
        assertEquals("baz", p2100.nav?.bruker?.bank?.konto?.sepa?.swift)

        assertEquals("BAMSE LUR", p2100.nav?.bruker?.person?.fornavn)
        assertEquals("MOMBALO", p2100.nav?.bruker?.person?.etternavn)
        val navfnr1 = Fodselsnummer.fra(p2100.nav?.bruker?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(75, navfnr1?.getAge())
        assertEquals("M", p2100.nav?.bruker?.person?.kjoenn)

        assertNotNull(p2100.nav?.bruker?.person?.pin)
        val pinlist = p2100.nav?.bruker?.person?.pin
        val pinitem = pinlist?.get(0)
        assertEquals(null, pinitem?.sektor)
        assertEquals("NOINST002, NO INST002, NO", pinitem?.institusjonsnavn)
        assertEquals("NO:noinst002", pinitem?.institusjonsid)
        assertEquals(avdodPersonFnr, pinitem?.identifikator)

        assertEquals("BAMSE ULUR", p2100.pensjon?.gjenlevende?.person?.fornavn)
        assertEquals("DOLLY", p2100.pensjon?.gjenlevende?.person?.etternavn)
        val navfnr2 = Fodselsnummer.fra(p2100.pensjon?.gjenlevende?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(65, navfnr2?.getAge())
        assertEquals("K", p2100.pensjon?.gjenlevende?.person?.kjoenn)

    }

}

