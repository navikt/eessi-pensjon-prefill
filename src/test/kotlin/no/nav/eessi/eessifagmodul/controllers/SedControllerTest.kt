package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.prefill.PrefillFactory
import no.nav.eessi.eessifagmodul.prefill.PrefillSED
import no.nav.eessi.eessifagmodul.services.LandkodeService
import no.nav.eessi.eessifagmodul.services.PrefillService
import no.nav.eessi.eessifagmodul.services.aktoerregister.AktoerregisterService
import no.nav.eessi.eessifagmodul.services.eux.EuxService
import no.nav.eessi.eessifagmodul.services.eux.RinaActions
import no.nav.eessi.eessifagmodul.services.personv3.PersonV3Service
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.http.HttpStatus
import org.springframework.web.util.UriComponentsBuilder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner.Silent::class)
class SedControllerTest {

    @Mock
    lateinit var mockEuxService: EuxService

    @Mock
    lateinit var mockAktoerregisterService: AktoerregisterService

    @Mock
    lateinit var mockRinaActions: RinaActions

    @Mock
    lateinit var mockPrefillService: PrefillService

    @Mock
    lateinit var mockPrefillSED: PrefillSED

    @Spy
    lateinit var landkodeService: LandkodeService

    private lateinit var prefillDataMock: PrefillDataModel
    private lateinit var sedController: SedController
    private lateinit var landkodeController: LandkodeController

    @Before
    fun setUp() {
        prefillDataMock = PrefillDataModel()
        mockPrefillService = PrefillService(mockEuxService, mockPrefillSED, mockRinaActions)
        this.sedController = SedController(mockEuxService, mockPrefillService, mockAktoerregisterService)
        this.landkodeController = LandkodeController(landkodeService)
    }

    @Test
    fun `create list landkoder`() {
        val response = landkodeController.getLandKoder()
        assertNotNull(response)
        assertEquals(31, response.size)
    }

    @Test
    fun `create frontend request`() {
        val json = "{\"institutions\":[{\"country\":\"NO\",\"institution\":\"DUMMY\"}],\"buc\":\"P_BUC_06\",\"sed\":\"P6000\",\"sakId\":\"123456\",\"aktoerId\":\"0105094340092\"}"
        //map json request back to FrontendRequest obj
        val map = jacksonObjectMapper()
        val req = map.readValue(json, SedController.ApiRequest::class.java)
        assertEquals("P_BUC_06", req.buc)
        assertEquals("DUMMY", req.institutions!![0].institution)
        assertEquals("123456", req?.sakId)
    }

    @Test
    fun `create document`() {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        val mockResponse = "1234567890"

        val requestMock = SedController.ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                euxCaseId = mockResponse,
                vedtakId = "1234567",
                institutions = items,
                sed = "P6000",
                buc = "P_BUC_06",
                aktoerId = "0105094340092"
        )

        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(ArgumentMatchers.anyString())).thenReturn("12345")
        val utfyllMock = sedController.buildPrefillDataModelOnNew(requestMock)

        assertNotNull(utfyllMock.personNr)
        assertEquals("12345", utfyllMock.personNr)

        whenever(mockPrefillSED.prefill(any())).thenReturn(utfyllMock)
        //whenever(mockEuxService.createCaseAndDocument(any(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(mockResponse)
        whenever(mockEuxService.createCaseWithDocument(any(), any(), any())).thenReturn(mockResponse)
        whenever(mockRinaActions.canCreate(anyString(), anyString())).thenReturn(true)
        whenever(mockRinaActions.canUpdate(utfyllMock.getSEDid(), mockResponse)).thenReturn(true)

        val response = sedController.createDocument(requestMock)
        assertEquals("{\"euxcaseid\":\"$mockResponse\"}", response)
    }

    @Test
    fun `validate addDocument SED on exisiting buc`() {

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))

        val requestMock = SedController.ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                euxCaseId = "1234567890",
                vedtakId = "1234567",
                institutions = items,
                sed = "P6000",
                buc = "P_BUC_06",
                aktoerId = "0105094340092"
        )

        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(ArgumentMatchers.anyString())).thenReturn("12345")
        val utfyllMock = sedController.buildPrefillDataModelOnExisting(requestMock)

        assertNotNull(utfyllMock.personNr)
        assertEquals("12345", utfyllMock.personNr)

        whenever(mockPrefillSED.prefill(any())).thenReturn(utfyllMock)
        whenever(mockEuxService.createSEDonExistingRinaCase(any(), any(), any())).thenReturn(HttpStatus.OK)
        whenever(mockRinaActions.canCreate(anyString(), anyString())).thenReturn(true)
        whenever(mockRinaActions.canUpdate(utfyllMock.getSEDid(), utfyllMock.euxCaseID)).thenReturn(true)

        val response = sedController.addDocument(requestMock)

        assertEquals("{\"euxcaseid\":\"${utfyllMock.euxCaseID}\"}", response)
    }

    @Test(expected = SedDokumentIkkeGyldigException::class)
    fun `addDocument SED on exisiting buc faild canCreate false`() {

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))

        val requestMock = SedController.ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                euxCaseId = "1234567890",
                vedtakId = "1234567",
                institutions = items,
                sed = "P6000",
                buc = "P_BUC_06",
                aktoerId = "0105094340092"
        )

        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(ArgumentMatchers.anyString())).thenReturn("12345")
        val utfyllMock = sedController.buildPrefillDataModelOnExisting(requestMock)

        assertNotNull(utfyllMock.personNr)
        assertEquals("12345", utfyllMock.personNr)

        whenever(mockPrefillSED.prefill(any())).thenReturn(utfyllMock)
        whenever(mockRinaActions.canCreate(anyString(), anyString())).thenReturn(false)
        sedController.addDocument(requestMock)
    }

    @Test(expected = SedDokumentIkkeOpprettetException::class)
    fun `addDocument SED on exisiting buc faild canUpdate false`() {

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))

        val requestMock = SedController.ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                euxCaseId = "1234567890",
                vedtakId = "1234567",
                institutions = items,
                sed = "P6000",
                buc = "P_BUC_06",
                aktoerId = "0105094340092"
        )

        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(ArgumentMatchers.anyString())).thenReturn("12345")
        val utfyllMock = sedController.buildPrefillDataModelOnExisting(requestMock)

        assertNotNull(utfyllMock.personNr)
        assertEquals("12345", utfyllMock.personNr)

        whenever(mockPrefillSED.prefill(any())).thenReturn(utfyllMock)
        whenever(mockEuxService.createSEDonExistingRinaCase(any(), any(), any())).thenReturn(HttpStatus.OK)
        whenever(mockRinaActions.canCreate(anyString(), anyString())).thenReturn(true)
        whenever(mockRinaActions.canUpdate(utfyllMock.getSEDid(), utfyllMock.euxCaseID)).thenReturn(false)
        sedController.addDocument(requestMock)
    }



    @Test(expected = SedDokumentIkkeOpprettetException::class)
    fun `create document fail on confirmUpdate`() {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))

        val requestMock = SedController.ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                vedtakId = "123456",
                institutions = items,
                euxCaseId = "1234567",
                sed = "P6000",
                buc = "P_BUC_02",
                aktoerId = "0105094340092"
        )
        val mockResponse = "1234567890"

        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(ArgumentMatchers.anyString())).thenReturn("12345")

        val utfyllMock = sedController.buildPrefillDataModelOnNew(requestMock)
        assertNotNull(utfyllMock.personNr)
        assertEquals("12345", utfyllMock.personNr)

        whenever(mockEuxService.createCaseAndDocument(any(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(mockResponse)
        whenever(mockRinaActions.canUpdate(anyString(), anyString())).thenReturn(false)
        whenever(mockPrefillSED.prefill(any())).thenReturn(utfyllMock)

        sedController.createDocument(requestMock)

    }

    @Test
    fun `confirm document`() {
        val mockData = SedController.ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                vedtakId = "1234567",
                institutions = listOf(InstitusjonItem("NO", "DUMMY")),
                euxCaseId = "1234567890",
                sed = "P6000",
                buc = "P_BUC_06",
                aktoerId = "0105094340092"
        )
        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(ArgumentMatchers.anyString())).thenReturn("12345")

        val utfyllMock = sedController.buildPrefillDataModelConfirm(mockData)

        utfyllMock.sed.nav = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy")))
        whenever(mockPrefillSED.prefill(any())).thenReturn(utfyllMock)

        val response = sedController.confirmDocument(mockData)
        //val response = SED.fromJson(response2)

        assertNotNull(response)
        assertEquals("P6000", response.sed)
        assertEquals("Dummy", response.nav?.bruker?.person?.fornavn)
        assertEquals("Dummy", response.nav?.bruker?.person?.etternavn)
    }

    @Test(expected = SedDokumentIkkeGyldigException::class)
    fun `confirm document when sed is not valid`() {
        val mockData = SedController.ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                institutions = listOf(InstitusjonItem("NO", "DUMMY")),
                sed = "Q3300",
                buc = "P_BUC_06",
                aktoerId = "0105094340092"
        )
        sedController.buildPrefillDataModelConfirm(mockData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `confirm document sed is null`() {
        val mockData = SedController.ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                institutions = listOf(InstitusjonItem("NO", "DUMMY")),
                sed = null,
                buc = "P_BUC_06",
                aktoerId = "0105094340092"
        )
        sedController.buildPrefillDataModelConfirm(mockData)
    }

    @Test
    fun `check on minimum valid request to model`() {
        val mockData = SedController.ApiRequest(
                sakId = "12234",
                sed = "P6000",
                aktoerId = "0105094340092"
        )
        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(ArgumentMatchers.anyString())).thenReturn("12345")
        val model = sedController.buildPrefillDataModelConfirm(mockData)

        assertEquals("12345", model.personNr)
        assertEquals("12234", model.penSaksnummer)
        assertEquals("0105094340092", model.aktoerID)
        assertEquals("P6000", model.getSEDid())
        assertEquals(SED::class.java, model.sed::class.java)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `check on aktoerId is null`() {
        val mockData = SedController.ApiRequest(
                sakId = "1213123123",
                sed = "P6000",
                aktoerId = null
        )
        sedController.buildPrefillDataModelConfirm(mockData)
    }

    @Test
    fun `check rest api path correct`() {
        val path = "/sed/get/{rinanr}/{documentid}"
        val uriParams = mapOf("rinanr" to "123456789", "documentid" to "DOC1223213234234")
        val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)
        assertEquals("/sed/get/123456789/DOC1223213234234", builder.path)
    }

}