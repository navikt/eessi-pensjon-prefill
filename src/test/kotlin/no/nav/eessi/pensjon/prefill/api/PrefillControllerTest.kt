
package no.nav.eessi.pensjon.prefill.api

import io.mockk.every
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.Krav
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.Person
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.prefill.ApiRequest
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonDataService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PrefillService
import no.nav.eessi.pensjon.prefill.models.InstitusjonItem
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate

class PrefillControllerTest {

    @SpyK
    var auditLogger: AuditLogger = AuditLogger()
    var mockPrefillSEDService: PrefillSEDService = mockk()
    var kafkaTemplate: KafkaTemplate<String, String> = mockk()
    var personDataService: PersonDataService = mockk()

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

        every {personDataService.hentFnrfraAktoerService(any())} returns "12345"

        val utfyllMock = ApiRequest.buildPrefillDataModelOnExisting(mockData, NorskIdent("12345").id, null)

        every{
            personDataService.hentPersonData(any())
        } returns( PersonDataCollection(PersonPDLMock.createWith(), PersonPDLMock.createWith()))

        val nav = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy", foedselsdato = "1900-10-11", kjoenn = "K")), krav = Krav("1937-12-11"))
        val mockSed = SED(
            type = utfyllMock.sedType,
            nav = nav
        )

        every{mockPrefillSEDService.prefill(any(), any())} returns mockSed

        val response = prefillController.prefillDocument(mockData)
        Assertions.assertNotNull(response)

        val sed = SED.fromJson(response)

        assertEquals(SedType.P6000, sed.type)
        assertEquals("Dummy", sed.nav?.bruker?.person?.fornavn)
        assertEquals("Dummy", sed.nav?.bruker?.person?.etternavn)
    }
}


