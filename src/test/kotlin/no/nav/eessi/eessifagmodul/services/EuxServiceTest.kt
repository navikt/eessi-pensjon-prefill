package no.nav.eessi.eessifagmodul.services

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.services.eux.EuxService
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.typeRef
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.util.*


@RunWith(MockitoJUnitRunner::class)
class EuxServiceTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EuxServiceTest::class.java) }

    private lateinit var service: EuxService

    private val objMapper = jacksonObjectMapper()

    @Mock
    private lateinit var mockrestTemplate: RestTemplate


    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        objMapper.enable(SerializationFeature.WRAP_ROOT_VALUE)
        service = EuxService(mockrestTemplate)
    }

    private fun createSendSEDmedFeilResponse(data: String, status: HttpStatus) {
        val response: ResponseEntity<String> = ResponseEntity(data, status)
        whenever(mockrestTemplate.getForEntity<String>(anyString(), any())).thenReturn(response)

        val rinaSaksnr = "12132123"
        service.getPossibleActions(rinaSaksnr)
    }

    @Test
    fun `check for mulige aksjoner on rinacaseid`() {
        val bucType = "FB_BUC_01"

        val data = "[" +
                "{\"navn\":\"Create\"," +
                "\"id\":\"312430_f54d4c4ea29840a3bd8404ec08ffd29f\",\n" +
                "\"kategori\":\"Documents\",\n" +
                "\"dokumentType\":\"P2000\"," +
                "\"dokumentId\":\"602982a0a84d4fe6aaf46a61b30a3a2e\"}]"
        val response: ResponseEntity<String> = ResponseEntity(data, HttpStatus.OK)
        whenever(mockrestTemplate.exchange(anyString(),eq(HttpMethod.GET), any(), eq(typeRef<String>()))).thenReturn(response)

        val resultat = service.getPossibleActions(bucType)

        assertNotNull(resultat)

        val aksjon = resultat[0]

        assertNotNull(aksjon)
        assertEquals("P2000", aksjon.dokumentType)
        assertEquals("Create", aksjon.navn)

    }

    @Test
    fun createCaseAndDocument() {

        val fagSaknr = "EESSI-PEN-123"
        val mottaker = "DUMMY"

        val sed = SedMock().genererP6000Mock()
        val sedAsJson = mapAnyToJson(sed)

        val bucType = "P_BUC_06" //p6000
        //val bucType = "P_BUC_01" //p2000

        val korrid = UUID.randomUUID()
        val vedleggType = ""

        //mock response and mock restTemplate
        val response: ResponseEntity<String> = ResponseEntity("12345", HttpStatus.OK)
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenReturn(response)

        try {
            val data = service.createCaseAndDocument(sedAsJson, bucType, fagSaknr, mottaker, vedleggType, korrid.toString())
            assertTrue("Skal komme hit!", true)
            logger.info("Response: $data")
            assertEquals("12345", data)
            assertNotNull(data)
            println("RINA NR?       : $data")
            println("KorrelasjonsID : $korrid")
            println("BuCType        : $bucType")
        } catch (ex: Exception) {
            logger.error(ex.message)
            fail("Skal ikke komme hit")
        }
    }

}

