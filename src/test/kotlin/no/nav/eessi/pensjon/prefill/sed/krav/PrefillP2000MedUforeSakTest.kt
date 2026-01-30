package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteService
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.models.YtelseskomponentType
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

class PrefillP2000MedUforeSakTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)
    private val pesysSaksnummer = "22874955"
    private val pesysService : PesysService = mockk()

    lateinit var prefillData: PrefillDataModel
    lateinit var etterlatteService: EtterlatteService
    private lateinit var personDataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
//        every { pesysService.hentP2000data(any()) } returns mockk(){
//            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
//                sakType = EessiSakType.AFP_PRIVAT,
//                kravHistorikk = listOf(
//                    P2xxxMeldingOmPensjonDto.KravHistorikk(
//                        mottattDato = LocalDate.of(2015, 11, 25),
//                        kravType = EessiFellesDto.EessiKravGjelder.F_BH_KUN_UTL,
//                        virkningstidspunkt = LocalDate.of(2015, 11, 25),
//                    )
//                ),
//                ytelsePerMaaned = emptyList(),
//                forsteVirkningstidspunkt = LocalDate.of(2025, 12, 12),
//                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
//            )
//            every { vedtak } returns P2xxxMeldingOmPensjonDto.Vedtak(boddArbeidetUtland = true)
//        }
        etterlatteService = mockk()
        personDataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, personFnr, penSaksnummer = pesysSaksnummer)
    }

    @Test
    fun `forventer exception - ikke relevant saktype for krav-SED - uforesak ikke relevant for P2000`() {
        val innhentingService = InnhentingService(mockk(), pesysService = mockk())

        assertThrows<ResponseStatusException> {
              innhentingService.hentPensjoninformasjonCollection(prefillData)
        }
    }
}

