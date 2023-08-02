package no.nav.eessi.pensjon.prefill
import io.mockk.mockk
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.statistikk.AutomatiseringStatistikkService
import org.junit.jupiter.api.BeforeEach

class PrefillServiceTest {

    private val mockPrefillSEDService: PrefillSEDService = mockk()
    private val innhentingService: InnhentingService = mockk()
    private val automatiseringStatistikkService: AutomatiseringStatistikkService = mockk()

    private lateinit var prefillService: PrefillService
    private lateinit var personcollection: PersonDataCollection

    @BeforeEach
    fun `startup initilize testing`() {
        prefillService = PrefillService(mockPrefillSEDService, innhentingService, automatiseringStatistikkService)
        personcollection = PersonDataCollection(null, null)
    }
}
