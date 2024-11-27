package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException

class PrefillP2000MedUforeSakTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)
    private val pesysSaksnummer = "22874955"

    lateinit var prefillData: PrefillDataModel
    lateinit var prefillNav: PrefillPDLNav
    lateinit var dataFromPEN: PensjonsinformasjonService
    lateinit var prefillSEDService: PrefillSEDService
    private lateinit var personDataCollection: PersonDataCollection
    private lateinit var pensjonCollection: PensjonCollection

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

        dataFromPEN = lesPensjonsdataFraFil("/pensjonsinformasjon/krav/P2200-UP-INNV.xml")

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.SEDTYPE_P2000, personFnr, penSaksnummer = pesysSaksnummer)


        prefillSEDService = PrefillSEDService(EessiInformasjon(), prefillNav)
    }

    @Test
    fun `forventer exception - ikke relevant saktype for krav-SED - uforesak ikke relevant for P2000`() {
        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)

        assertThrows<ResponseStatusException> {
              innhentingService.hentPensjoninformasjonCollection(prefillData)
        }
    }
}

