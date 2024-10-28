package no.nav.eessi.pensjon.prefill
import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.EessiInformasjonMother
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.shared.api.PersonId
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.statistikk.AutomatiseringStatistikkService
import org.junit.Before
import org.junit.Test

class PrefillServiceTest {
    private val personFnr = FodselsnummerGenerator.generateFnrForTest(57)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(63)

    private val mockPrefillSEDService: PrefillSEDService = mockk()
    private val innhentingService: InnhentingService = mockk()
    private val krrService: KrrService = mockk()
    private val automatiseringStatistikkService: AutomatiseringStatistikkService = mockk()
    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var prefillService: PrefillService
    private lateinit var personcollection: PersonDataCollection
    private lateinit var personDataCollection: PersonDataCollection
    private lateinit var prefillNav: PrefillPDLNav
    private lateinit var eessiInformasjon: EessiInformasjon

    @Before
    fun setup() {
        prefillService = PrefillService(krrService, mockPrefillSEDService, innhentingService, automatiseringStatistikkService)
        personcollection = PersonDataCollection(null, null)
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

        eessiInformasjon = EessiInformasjonMother.standardEessiInfo()
    }

    @Test
    fun `En p6000 uten vedtak skal gi en delvis utfylt sed`(){
        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("/pensjonsinformasjon/vedtak/P6000-GP-401.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312", avdod = PersonId(avdodPersonFnr, "1234567891234"))
        prefillSEDService = PrefillSEDService(eessiInformasjon, prefillNav)
    }
}
