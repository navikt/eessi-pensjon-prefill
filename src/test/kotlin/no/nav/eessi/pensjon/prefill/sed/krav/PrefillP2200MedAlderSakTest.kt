package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.personoppslag.pdl.model.Sivilstandstype
import no.nav.eessi.pensjon.prefill.FeilSakstypeForSedException
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.models.eessi.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.pdl.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.prefill.models.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PrefillP2200MedAlderSakTest {

    private val personFnr = generateRandomFnr(68)
    private val ekteFnr = generateRandomFnr(70)
    private val pesysSaksnummer = "14069110"

    private lateinit var prefillData: PrefillDataModel
    private lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var innhentingService: InnhentingService

    private lateinit var personDataCollection: PersonDataCollection


    @BeforeEach
    fun setup() {
        val person = PersonPDLMock.createWith(fornavn = "BAMSE ULUR", fnr = personFnr)
        val ekte = PersonPDLMock.createWith(fornavn = "BAMSE LUR", fnr = ekteFnr)

        personDataCollection = PersonDataCollection(
            forsikretPerson = person,
            sivilstandstype = Sivilstandstype.GIFT,
            gjenlevendeEllerAvdod = person,
            ektefellePerson = ekte
        )

        val prefillNav = PrefillPDLNav(
                prefillAdresse = mockk(),
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")

        dataFromPEN = lesPensjonsdataFraFil("PensjonsinformasjonSaksliste-AP-14069110.xml")

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P2200, personFnr, penSaksnummer = pesysSaksnummer)

        prefillSEDService = PrefillSEDService(EessiInformasjon(), prefillNav)

        innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        //val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        //pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

    }

    @Test
    fun `forventer exception - ikke relevant saktype for krav-SED - aldersak ikke relevant for P2200`() {
        assertThrows<FeilSakstypeForSedException> {
            innhentingService.hentPensjoninformasjonCollection(prefillData)
        }
    }

}

