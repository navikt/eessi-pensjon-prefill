
package no.nav.eessi.pensjon.fagmodul.api

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.fagmodul.eux.EuxInnhentingService
import no.nav.eessi.pensjon.fagmodul.eux.EuxPrefillService
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.PersonDataCollection
import no.nav.eessi.pensjon.fagmodul.prefill.*
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.kafka.core.KafkaTemplate

@ExtendWith(MockitoExtension::class)
class PrefillControllerTest {

    @Spy
    lateinit var auditLogger: AuditLogger

    @Spy
    lateinit var mockEuxPrefillService: EuxPrefillService

    @Mock
    lateinit var mockPrefillSEDService: PrefillSEDService

    @Spy
    lateinit var mockEuxInnhentingService: EuxInnhentingService
    
    @Mock
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Mock
    private lateinit var personDataService: PersonDataService

    private lateinit var prefillController: PrefillController

    @BeforeEach
    fun before() {

        val innhentingService = InnhentingService(personDataService)
        innhentingService.initMetrics()

        val prefillService = PrefillService(mockPrefillSEDService)
        prefillService.initMetrics()

        prefillController = PrefillController(
            innhentingService,
            prefillService,
            auditLogger
        )

        prefillController.initMetrics()
    }

    @Test
    fun `confirm document`() {
        val mockData = ApiRequest(
            subjectArea = "Pensjon",
            sakId = "EESSI-PEN-123",
            vedtakId = "1234567",
            institutions = listOf(InstitusjonItem("NO", "DUMMY")),
            euxCaseId = "1234567890",
            sed = "P6000",
            buc = "P_BUC_06",
            aktoerId = "0105094340092"
        )

        doReturn("12345").whenever(personDataService).hentFnrfraAktoerService(any())

        val utfyllMock = ApiRequest.buildPrefillDataModelOnExisting(mockData, NorskIdent("12345").id, null)

        doReturn(PersonDataCollection(PersonPDLMock.createWith(), PersonPDLMock.createWith())).whenever(personDataService).hentPersonData(any())

        val nav = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy", foedselsdato = "1900-10-11", kjoenn = "K")), krav = Krav("1937-12-11"))
        val mockSed = SED(
            type = utfyllMock.sedType,
            nav = nav
        )

        doReturn(mockSed).whenever(mockPrefillSEDService).prefill(any(), any())

        val response = prefillController.prefillDocument(mockData)
        Assertions.assertNotNull(response)

        val sed = SED.fromJson(response)

        assertEquals(SedType.P6000, sed.type)
        assertEquals("Dummy", sed.nav?.bruker?.person?.fornavn)
        assertEquals("Dummy", sed.nav?.bruker?.person?.etternavn)
    }
}

