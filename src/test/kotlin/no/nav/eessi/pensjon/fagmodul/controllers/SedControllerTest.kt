package no.nav.eessi.pensjon.fagmodul.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.pensjon.helper.AktoerIdHelper
import no.nav.eessi.pensjon.fagmodul.models.*
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillSED
import no.nav.eessi.pensjon.fagmodul.prefill.nav.PrefillPerson
import no.nav.eessi.pensjon.fagmodul.services.ApiRequest
import no.nav.eessi.pensjon.fagmodul.services.PrefillService
import no.nav.eessi.pensjon.fagmodul.services.eux.BucSedResponse
import no.nav.eessi.pensjon.fagmodul.services.eux.BucUtils
import no.nav.eessi.pensjon.fagmodul.services.eux.EuxService
import no.nav.eessi.pensjon.fagmodul.services.eux.SedDokumentIkkeOpprettetException
import no.nav.eessi.pensjon.fagmodul.services.eux.bucmodel.ShortDocumentItem
import no.nav.eessi.pensjon.utils.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.http.ResponseEntity
import org.springframework.web.util.UriComponentsBuilder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner.Silent::class)
class SedControllerTest {

    @Mock
    lateinit var mockEuxService: EuxService

    @Mock
    lateinit var mockAktoerIdHelper: AktoerIdHelper

    @Mock
    lateinit var mockPrefillSED: PrefillSED

    private lateinit var prefillDataMock: PrefillDataModel
    private lateinit var sedController: SedController

    @Before
    fun setUp() {
        prefillDataMock = PrefillDataModel()
        this.sedController = SedController(mockEuxService, PrefillService(mockPrefillSED), mockAktoerIdHelper)
    }

    @Test
    fun `create frontend request`() {
        val json = "{\"institutions\":[{\"country\":\"NO\",\"institution\":\"DUMMY\"}],\"buc\":\"P_BUC_06\",\"sed\":\"P6000\",\"sakId\":\"123456\",\"aktoerId\":\"0105094340092\"}"

        //map json request back to FrontendRequest obj
        val map = jacksonObjectMapper()
        val req = map.readValue(json, ApiRequest::class.java)


        assertEquals("P_BUC_06", req.buc)
        assertEquals("DUMMY", req.institutions!![0].institution)
        assertEquals("123456", req?.sakId)
    }

    @Test
    fun `calling createDocument | forventer BucSedResponse (euxCaseid, documentid) ved mockedResponse`() {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        val mockResponse = BucSedResponse("1234567890", "123123123-123123123-123131")

        val requestMock = ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                euxCaseId = mockResponse.caseId,
                vedtakId = "1234567",
                institutions = items,
                sed = "P6000",
                buc = "P_BUC_06",
                aktoerId = "0105094340092"
        )

        //må være først
        doReturn("12345").whenever(mockAktoerIdHelper).hentAktoerIdPin(ArgumentMatchers.anyString())

        val utfyllMock = ApiRequest.buildPrefillDataModelOnNew(requestMock, mockAktoerIdHelper)

        assertNotNull(utfyllMock.personNr)
        assertEquals("12345", utfyllMock.personNr)

        //mock prefillSED
        utfyllMock.sed.nav = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy", foedselsdato = "1900-10-11", kjoenn = "K")), krav = Krav("1937-12-11"))
        doReturn(utfyllMock).whenever(mockPrefillSED).prefill(any())

        //mock opprett type og sed til RINA
        doReturn(mockResponse).whenever(mockEuxService).opprettBucSed(
                any(),
                any(),
                any(),
                any()
        )

        val response = sedController.createDocument(requestMock)

        assertEquals("1234567890", response.caseId)
        assertEquals("123123123-123123123-123131", response.documentId)
    }

    @Test
    fun `forventer alt ok ved å legge til en ny SED på en ekisternede buc `() {
        val bucresponse = BucSedResponse("123444455", "2a427c10325c4b5eaf3c27ba5e8f1877")

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        val requestMock = ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                euxCaseId = "1234567890",
                vedtakId = "1234567",
                institutions = items,
                sed = "P6000",
                buc = "P_BUC_06",
                aktoerId = "0105094340092"
        )

        whenever(mockAktoerIdHelper.hentAktoerIdPin(ArgumentMatchers.anyString())).thenReturn("12345")
        val utfyllMock = ApiRequest.buildPrefillDataModelOnExisting(requestMock, mockAktoerIdHelper)

        assertNotNull(utfyllMock.personNr)
        assertEquals("12345", utfyllMock.personNr)

        utfyllMock.sed.nav = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy", foedselsdato = "1900-10-11", kjoenn = "K")), krav = Krav("1937-12-11"))

        val mockShortDoc = ShortDocumentItem(id = "2a427c10325c4b5eaf3c27ba5e8f1877", type = "P6000", status = "Nadada")
        //val buc = Buc(id = "123123", documents = listOf(DocumentsItem(id = "2a427c10325c4b5eaf3c27ba5e8f1877", type = "P6000", status = "Nada")))

        whenever(mockEuxService.addDeltagerInstitutions(any(), any())).thenReturn(true)
        whenever(mockPrefillSED.prefill(any())).thenReturn(utfyllMock)
        whenever(mockEuxService.opprettSedOnBuc(any(), any())).thenReturn(bucresponse)

        val mockbuc = Mockito.mock(BucUtils::class.java)

        whenever(mockbuc.findDocument(any())).thenReturn(mockShortDoc)
        whenever(mockEuxService.getBucUtils(any())).thenReturn(mockbuc)


        val response = sedController.addDocument(requestMock)
        //assertEquals("123444455", response.caseId)
        assertEquals("2a427c10325c4b5eaf3c27ba5e8f1877", response.id)
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
        whenever(mockAktoerIdHelper.hentAktoerIdPin(ArgumentMatchers.anyString())).thenReturn("12345")

        val utfyllMock = ApiRequest.buildPrefillDataModelConfirm(mockData, mockAktoerIdHelper)

        utfyllMock.sed.nav = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy", foedselsdato = "1900-10-11", kjoenn = "K")), krav = Krav("1937-12-11"))
        whenever(mockPrefillSED.prefill(any())).thenReturn(utfyllMock)

        val response = sedController.confirmDocument(mockData)
        //val response = SED.fromJson(response2)

        assertNotNull(response)
        assertEquals("P6000", response.sed)
        assertEquals("Dummy", response.nav?.bruker?.person?.fornavn)
        assertEquals("Dummy", response.nav?.bruker?.person?.etternavn)
    }


    @Test
    fun `check rest api path correct`() {
        val path = "/sed/get/{rinanr}/{documentid}"
        val uriParams = mapOf("rinanr" to "123456789", "documentid" to "DOC1223213234234")
        val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)
        assertEquals("/sed/get/123456789/DOC1223213234234", builder.path)
    }

    @Test
    fun `Test liste med SED kun PensjonSED skal returneres`() {
        val list  = listOf("X005","P2000","P4000","H02","X06","P9000", "")

        val result = list.filterPensionSedAndSort()

        assertEquals(3, result.size)
        assertEquals("[P2000, P4000, P9000]", result.toString())
    }

    @Test
    fun `Test liste med SED som skal returneres`() {
        val list  = listOf("X005","P2000","P4000","H02","H070", "X06", "H121","P9000", "")

        val result = list.filterPensionSedAndSort()

        println(result.toString())

        assertEquals(5, result.size)
        assertEquals("[H070, H121, P2000, P4000, P9000]", result.toString())
    }


    @Test
    fun `Test av liste med SEDer der kun PensjonSEDer skal returneres`() {
        val list  = listOf("X005","P2000","P4000","H02","X06","P9000", "")

        val result = list.filterPensionSedAndSort()

        assertEquals(3, result.size)
        assertEquals("[ \"P2000\", \"P4000\", \"P9000\" ]", mapAnyToJson(result))
    }

    @Test
    fun `Calling euxController|getSeds on BUC01 returns SEDs for a given BUC`() {
        val buc = "P_BUC_01"
        val rinanr = null

        val expectedResponse = ResponseEntity.ok().body(mapAnyToJson(listOf("P2000")))
        val generatedResponse = sedController.getSeds(buc, rinanr)
        assertEquals(expectedResponse, generatedResponse)
    }

    @Test
    fun `Calling euxController|getSeds on BUC02 returns SEDs for a given BUC`() {
        val buc = "P_BUC_02"
        val rinanr = null

        val expectedResponse = ResponseEntity.ok().body(mapAnyToJson(listOf("P2100")))
        val generatedResponse = sedController.getSeds(buc, rinanr)
        assertEquals(expectedResponse, generatedResponse)
    }

    @Test
    fun `Calling euxController|getSeds on BUC03 returns SEDs for a given BUC`() {
        val buc = "P_BUC_03"
        val rinanr = null

        val expectedResponse = ResponseEntity.ok().body(mapAnyToJson(listOf("P2200")))
        val generatedResponse = sedController.getSeds(buc, rinanr)
        assertEquals(expectedResponse, generatedResponse)
    }


    @Test
    fun `Calling euxController|getSeds on BUC05 returns SEDs for a given BUC`() {
        val buc = "P_BUC_05"
        val rinanr = null

        val expectedResponse = ResponseEntity.ok().body(mapAnyToJson(listOf("P5000", "P6000", "P7000", "P8000", "P9000")))
        val generatedResponse = sedController.getSeds(buc, rinanr)
        assertEquals(expectedResponse, generatedResponse)
    }

    @Test
    fun `Calling euxController|getSeds on BUC06 returns SEDs for a given BUC`() {
        val buc = "P_BUC_06"
        val rinanr = null

        val expectedResponse = ResponseEntity.ok().body(mapAnyToJson(listOf("P5000", "P6000", "P7000", "P10000")))
        val generatedResponse = sedController.getSeds(buc, rinanr)
        assertEquals(expectedResponse, generatedResponse)
    }

    @Test
    fun `Calling euxController|getSeds on BUC10 returns SEDs for a given BUC`() {
        val buc = "P_BUC_10"
        val rinanr = null

        val expectedResponse = ResponseEntity.ok().body(mapAnyToJson(listOf("P15000")))
        val generatedResponse = sedController.getSeds(buc, rinanr)
        assertEquals(expectedResponse, generatedResponse)
    }

    @Test
    fun `Calling euxController|getSeds on blank returns all SEDs`() {
        val buc = null
        val rinanr = null

        val expectedResponse = ResponseEntity.ok().body(mapAnyToJson(listOf("P2000", "P2100", "P2200", "P5000", "P6000", "P7000", "P8000", "P9000", "P10000", "P15000")))
        val generatedResponse = sedController.getSeds(buc, rinanr)
        assertEquals(expectedResponse, generatedResponse)
    }

    @Test
    fun euxController_getSeds_returnsSEDOnsgivenBUC() {
        val buc = "P_BUC_01"
        val rinanr = "1000101"

        val mockBucAksjonList =  listOf("P6000","X6000","X005","P2200","P3000_SE","P3000_NO")

        val mockGetBucUtils = Mockito.mock(BucUtils::class.java)
        whenever(mockGetBucUtils.getAksjonListAsString()).thenReturn(mockBucAksjonList)
        whenever(mockEuxService.getBucUtils(rinanr)).thenReturn(mockGetBucUtils)



        val expectedSedList = ResponseEntity.ok().body(mapAnyToJson( listOf("P2200", "P6000")))
        val generatedResponse = sedController.getSeds(buc, rinanr)

        assertEquals(expectedSedList, generatedResponse)

        val json = generatedResponse.body!!
        val validSedListforBuc = mapJsonToAny(json, typeRefs<List<String>>())
        assertEquals(2, validSedListforBuc.size)
    }

    @Test
    fun getYtelseKravtypeOk() {
        val mockKrav = PinOgKrav(fnr = "13212312", krav =  Krav(dato = "2019-02-01", type = "01"))

        doReturn(mockKrav).whenever(mockEuxService).
                hentFnrOgYtelseKravtype(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString()
                )

        val mockResult =  sedController.getPinOgYtelseKravtype("12123", "3123123")

        assertEquals("13212312", mockResult.fnr)
        assertEquals("01", mockResult.krav?.type)
        assertEquals("2019-02-01", mockResult.krav?.dato)
    }

    @Test
    fun `call addInstutionAndDocument| mock adding two institusjon when X005 exists already`() {
        val euxCaseId = "1234567890"

        doReturn("12345").whenever(mockAktoerIdHelper).hentAktoerIdPin(ArgumentMatchers.anyString())

        val mockBucUtils = Mockito.mock(BucUtils::class.java)
        whenever(mockEuxService.getBucUtils(euxCaseId)).thenReturn(mockBucUtils)

        val newParticipants = listOf(
                InstitusjonItem(country = "FI", institution = "Finland", name="Finland test"),
                InstitusjonItem(country = "DE", institution = "Tyskland", name="Tyskland test")
        )
        whenever(mockBucUtils.findNewParticipants(any())).thenReturn(newParticipants)

        val currentX005 = ShortDocumentItem()
        whenever(mockBucUtils.findFirstDocumentItemByType("X005")).thenReturn(currentX005)

        val dummyPrefillData = ApiRequest.buildPrefillDataModelOnExisting(apiRequestWith(euxCaseId), mockAktoerIdHelper)
        whenever(mockPrefillSED.prefill(any())).thenReturn(dummyPrefillData)

        whenever(mockEuxService.opprettSedOnBuc(any(), eq(euxCaseId))).thenReturn(BucSedResponse(euxCaseId, "1"))

        sedController.addInstutionAndDocument(apiRequestWith(euxCaseId))

        verify(mockEuxService, times(newParticipants.size + 1)).opprettSedOnBuc(any(), eq(euxCaseId))
        verify(mockEuxService, never()).addDeltagerInstitutions(any(), any())
    }

    @Test
    fun `call addInstutionAndDocument| ingen ny Deltaker kun hovedsed`() {
        val euxCaseId = "1234567890"

        doReturn("12345").whenever(mockAktoerIdHelper).hentAktoerIdPin(ArgumentMatchers.anyString())

        val mockBucUtils = Mockito.mock(BucUtils::class.java)
        whenever(mockEuxService.getBucUtils(euxCaseId)).thenReturn(mockBucUtils)

        val noNewParticipants = listOf<InstitusjonItem>()
        whenever(mockBucUtils.findNewParticipants(any())).thenReturn(noNewParticipants)

        val dummyPrefillData = ApiRequest.buildPrefillDataModelOnExisting(apiRequestWith(euxCaseId), mockAktoerIdHelper)
        whenever(mockPrefillSED.prefill(any())).thenReturn(dummyPrefillData)

        whenever(mockEuxService.opprettSedOnBuc(any(), eq(euxCaseId))).thenReturn(BucSedResponse(euxCaseId, "1"))

        sedController.addInstutionAndDocument(apiRequestWith(euxCaseId))

        verify(mockEuxService, times(noNewParticipants.size + 1)).opprettSedOnBuc(any(), eq(euxCaseId))
        verify(mockEuxService, never()).addDeltagerInstitutions(any(), any())
    }

    @Test
    fun `call addInstutionAndDocument| to nye deltakere, men ingen X005`() {
        val euxCaseId = "1234567890"

        doReturn("12345").whenever(mockAktoerIdHelper).hentAktoerIdPin(ArgumentMatchers.anyString())

        val mockBucUtils = Mockito.mock(BucUtils::class.java)
        whenever(mockEuxService.getBucUtils(euxCaseId)).thenReturn(mockBucUtils)

        val newParticipants = listOf(
                InstitusjonItem(country = "FI", institution = "Finland", name="Finland test"),
                InstitusjonItem(country = "DE", institution = "Tyskland", name="Tyskland test")
        )
        whenever(mockBucUtils.findNewParticipants(any())).thenReturn(newParticipants)

        val noCurrentX005 = null
        whenever(mockBucUtils.findFirstDocumentItemByType("X005")).thenReturn(noCurrentX005)

        val dummyPrefillData = ApiRequest.buildPrefillDataModelOnExisting(apiRequestWith(euxCaseId), mockAktoerIdHelper)
        whenever(mockPrefillSED.prefill(any())).thenReturn(dummyPrefillData)

        whenever(mockEuxService.opprettSedOnBuc(any(), eq(euxCaseId))).thenReturn(BucSedResponse(euxCaseId, "1"))

        sedController.addInstutionAndDocument(apiRequestWith(euxCaseId))

        verify(mockEuxService).addDeltagerInstitutions(euxCaseId, newParticipants)
        verify(mockEuxService, times(1)).opprettSedOnBuc(any(), eq(euxCaseId))
    }

    @Test(expected = SedDokumentIkkeOpprettetException::class)
    fun `call addInstutionAndDocument| Exception eller feil`() {
        val euxCaseId = "1234567890"

        doReturn("12345").whenever(mockAktoerIdHelper).hentAktoerIdPin(ArgumentMatchers.anyString())

        val mockBucUtils = Mockito.mock(BucUtils::class.java)
        whenever(mockEuxService.getBucUtils(euxCaseId)).thenReturn(mockBucUtils)

        val newParticipants = listOf(
                InstitusjonItem(country = "FI", institution = "Finland", name="Finland test"),
                InstitusjonItem(country = "DE", institution = "Tyskland", name="Tyskland test")
        )
        whenever(mockBucUtils.findNewParticipants(any())).thenReturn(newParticipants)

        val currentX005 = ShortDocumentItem()
        whenever(mockBucUtils.findFirstDocumentItemByType("X005")).thenReturn(currentX005)

        val dummyPrefillData = ApiRequest.buildPrefillDataModelOnExisting(apiRequestWith(euxCaseId), mockAktoerIdHelper)
        whenever(mockPrefillSED.prefill(any())).thenReturn(dummyPrefillData)

        whenever(mockEuxService.opprettSedOnBuc(any(), eq(euxCaseId))).thenThrow(SedDokumentIkkeOpprettetException("Expected!"))

        sedController.addInstutionAndDocument(apiRequestWith(euxCaseId))
    }

    private fun apiRequestWith(euxCaseId: String): ApiRequest {
        return ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                euxCaseId = euxCaseId,
                vedtakId = "1234567",
                institutions = listOf(),
                sed = "P6000",
                buc = "P_BUC_06",
                aktoerId = "0105094340092"
        )
    }


}