package no.nav.eessi.pensjon.prefill

import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.models.EessiInformasjonMother
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.statistikk.AutomatiseringStatistikkService
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

class PrefillSEDServiceTest {
    private val personFnr = FodselsnummerGenerator.generateFnrForTest(57)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(63)

    private val mockPrefillSEDService: PrefillSEDService = mockk()
    private val innhentingService: InnhentingService = mockk()
    private val krrService: KrrService = mockk()
    private val etterlatteService: EtterlatteService = mockk()
    private val automatiseringStatistikkService: AutomatiseringStatistikkService = mockk()
    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var prefillService: PrefillService
    private lateinit var personcollection: PersonDataCollection
    private lateinit var personDataCollection: PersonDataCollection
    private lateinit var prefillNav: PrefillPDLNav

    @Before
    fun setup() {
        prefillService = PrefillService(krrService, mockPrefillSEDService, innhentingService, etterlatteService, automatiseringStatistikkService, prefillNav)
        personcollection = PersonDataCollection(null, null)
        val personDataCollectionFamilie = PersonPDLMock.createEnkelFamilie(personFnr, avdodPersonFnr)
        personDataCollection = PersonDataCollection(gjenlevendeEllerAvdod = personDataCollectionFamilie.ektefellePerson, forsikretPerson = personDataCollectionFamilie.forsikretPerson )

    prefillNav = BasePrefillNav.createPrefillNav()
    }

    @Test
    fun `En p6000 uten vedtak skal gi en delvis utfylt sed`(){
        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("/pensjonsinformasjon/vedtak/P6000-GP-401.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312", avdod = PersonInfo(avdodPersonFnr, "1234567891234"))
        prefillSEDService = PrefillSEDService(EessiInformasjonMother.standardEessiInfo(), prefillNav)
        val prefill = prefillSEDService.prefillGjenny(prefillData, personDataCollection, null)

        assertNotNull(prefill.nav?.bruker?.person?.pin)
        assertEquals(avdodPersonFnr, prefill.nav?.bruker?.person?.pin?.firstOrNull()?.identifikator)
        assertEquals(SedType.P6000, prefill.type)
    }
}
