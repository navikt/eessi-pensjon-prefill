package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiKravGjelder
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

class PrefillP2200MedAlderSakTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)
    private val pesysSaksnummer = "14069110"
    private val pesysService : PesysService = mockk()

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var innhentingService: InnhentingService
    private lateinit var personDataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        every { pesysService.hentP2200data(any()) } returns mockk(){
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiSakType.ALDER,
                kravHistorikk = emptyList(),
                ytelsePerMaaned = emptyList(),
                forsteVirkningstidspunkt = LocalDate.of(2025, 12, 12),
                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
            )
        }

        val ekte = PersonPDLMock.createWith(fornavn = "BAMSE LUR", fnr = ekteFnr)
        val person = PersonPDLMock.createWith(fornavn = "BAMSE ULUR", fnr = personFnr)

        personDataCollection = PersonDataCollection(
            forsikretPerson = person,
            gjenlevendeEllerAvdod = person,
            ektefellePerson = ekte
        )

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P2200, personFnr, penSaksnummer = pesysSaksnummer)
        prefillSEDService = BasePrefillNav.createPrefillSEDService()
        innhentingService = InnhentingService(mockk(), pesysService = pesysService)
    }

    @Test
    fun `forventer exception - ikke relevant saktype for krav-SED - aldersak ikke relevant for P2200`() {
        assertThrows<ResponseStatusException> {
            innhentingService.hentPensjoninformasjonCollection(prefillData)
        }
    }

}

