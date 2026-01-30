package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiKravAarsak
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiKravGjelder
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.LocalDate

class PrefillP2100UforePRevurdering {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(45)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(45)
    private val pesysSaksnummer = "22917763"
    private val pesysKravid = "12354"
    private val pesysService: PesysService = mockk()

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService

    @BeforeEach
    fun setup() {
        every { pesysService.hentP2100data(any()) } returns mockk(){
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiSakType.UFOREP,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.of(2020, 8, 1),
                        kravType = EessiKravGjelder.F_BH_KUN_UTL,
                        virkningstidspunkt = LocalDate.of(2015, 11, 25),
                        kravArsak = EessiKravAarsak.GJNL_SKAL_VURD
                    )
                ),
                ytelsePerMaaned = emptyList(),
                forsteVirkningstidspunkt = LocalDate.of(2025, 12, 12),
                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
            )
            every { vedtak } returns P2xxxMeldingOmPensjonDto.Vedtak(boddArbeidetUtland = true)
        }

        prefillData = PrefillDataModelMother.initialPrefillDataModel(
                sedType = SedType.P2100,
                pinId = personFnr,
                penSaksnummer = pesysSaksnummer,
                avdod = PersonInfo(avdodPersonFnr, "112233445566"),
                kravId = pesysKravid)

    }

    @Test
    fun `forventet korrekt utfylt P2100 uforepensjon med kap4 og 9`() {
        val personDataCollection = PersonPDLMock.createAvdodFamilie(personFnr, avdodPersonFnr)

        val innhentingService = InnhentingService(mockk(), pesysService = pesysService)
        val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        prefillSEDService = BasePrefillNav.createPrefillSEDService()

        val p2100 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null,)

        assertNotNull(p2100.nav?.krav)
        assertEquals("2020-08-01", p2100.nav?.krav?.dato)
        assertEquals("Kravdato fra det opprinnelige vedtak med gjenlevenderett er angitt i SED P2100", prefillData.melding)
    }

}

