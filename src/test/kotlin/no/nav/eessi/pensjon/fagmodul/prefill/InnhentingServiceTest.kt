package no.nav.eessi.pensjon.fagmodul.prefill

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.Ident
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.server.ResponseStatusException


@ExtendWith(MockitoExtension::class)
class InnhentingServiceTest {

    @Mock
    private lateinit var personDataService: PersonDataService

    private lateinit var innhentingService: InnhentingService

    @BeforeEach
    fun before() {
        innhentingService = InnhentingService(personDataService)
        innhentingService.initMetrics()
    }

    @Test
    fun `call getAvdodAktoerId  expect valid aktoerId when avdodfnr exist and sed is P2100`() {
        val apiRequest = ApiRequest(
            subjectArea = "Pensjon",
            sakId = "EESSI-PEN-123",
            sed = "P2100",
            buc = "P_BUC_02",
            aktoerId = "0105094340092",
            avdodfnr = "12345566"

        )
        doReturn(AktoerId("1122334455")).whenever(personDataService).hentIdent(eq(IdentType.AktoerId), any<Ident<*>>())

        val result = innhentingService.getAvdodAktoerIdPDL(apiRequest)
        Assertions.assertEquals("1122334455", result)
    }

    @Test
    fun `call getAvdodAktoerId  expect valid aktoerId when avdod exist and sed is P5000`() {
        val apiRequest = ApiRequest(
            subjectArea = "Pensjon",
            sakId = "EESSI-PEN-123",
            sed = "P5000",
            buc = "P_BUC_02",
            aktoerId = "0105094340092",
            avdodfnr = "12345566",
            vedtakId = "23123123",
            subject = ApiSubject(gjenlevende = SubjectFnr("23123123"), avdod = SubjectFnr("46784678467"))
        )

        doReturn(AktoerId("467846784671")).whenever(personDataService).hentIdent(eq(IdentType.AktoerId), any<Ident<*>>())

        val result = innhentingService.getAvdodAktoerIdPDL(apiRequest)
        Assertions.assertEquals("467846784671", result)
    }

    @Test
    fun `call getAvdodAktoerId  expect error when avdodfnr is missing and sed is P2100`() {
        val apiRequest = ApiRequest(
            subjectArea = "Pensjon",
            sakId = "EESSI-PEN-123",
            sed = "P2100",
            buc = "P_BUC_02",
            aktoerId = "0105094340092"
        )
        assertThrows<MangelfulleInndataException> {
            innhentingService.getAvdodAktoerIdPDL(apiRequest)
        }
    }

    @Test
    fun `call getAvdodAktoerId expect error when avdodfnr is invalid and sed is P15000`() {
        val apiRequest = ApiRequest(
            subjectArea = "Pensjon",
            sakId = "EESSI-PEN-123",
            sed = "P15000",
            buc = "P_BUC_10",
            aktoerId = "0105094340092",
            avdodfnr = "12345566"
        )
        assertThrows<ResponseStatusException> {
            innhentingService.getAvdodAktoerIdPDL(apiRequest)
        }
    }

    @Test
    fun `call getAvdodAktoerId  expect null value when sed is P2000`() {
        val apireq = ApiRequest(
            subjectArea = "Pensjon",
            sakId = "EESSI-PEN-123",
            sed = "P2000",
            buc = "P_BUC_01",
            aktoerId = "0105094340092",
            avdodfnr = "12345566"
        )
        val result = innhentingService.getAvdodAktoerIdPDL(request = apireq)
        assertEquals(null, result)
    }


}