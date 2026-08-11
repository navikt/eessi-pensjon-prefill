package no.nav.eessi.pensjon.prefill

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteService
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteVedtakResponseData
import no.nav.eessi.pensjon.prefill.models.EessiInformasjonMother
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
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
    private var prefillSEDService: PrefillSEDService = mockk()
    private lateinit var prefillService: PrefillService
    private lateinit var personcollection: PersonDataCollection
    private lateinit var personDataCollection: PersonDataCollection
    private var prefillNav: PrefillPDLNav = mockk()

    @Before
    fun setup() {
        prefillService = PrefillService(
            krrService,
            mockPrefillSEDService,
            innhentingService,
            mockk(),
            automatiseringStatistikkService
        )
        personcollection = PersonDataCollection(null, null)
        val personDataCollectionFamilie = PersonPDLMock.createEnkelFamilie(personFnr, avdodPersonFnr)
        personDataCollection = PersonDataCollection(gjenlevendeEllerAvdod = personDataCollectionFamilie.ektefellePerson, forsikretPerson = personDataCollectionFamilie.forsikretPerson )
        prefillNav = BasePrefillNav.createPrefillNav()
    }

}
